package com.example.spotter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = "spotter.kafka.enabled=false")
class SpotterApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void exposesSeededSpacesForFrontend() throws Exception {
		mockMvc.perform(get("/api/spotter/spaces"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(100)))
				.andExpect(jsonPath("$[0].sensorId").exists())
				.andExpect(jsonPath("$[0].lotName").exists())
				.andExpect(jsonPath("$[0].occupied").exists());
	}

	@Test
	void runsNextSimulationEventFromCsvFeed() throws Exception {
		mockMvc.perform(post("/api/spotter/simulation/next")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.appliedEvents").value(1))
				.andExpect(jsonPath("$.feedSize", greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$.events", hasSize(1)))
				.andExpect(jsonPath("$.summary.totalSpaces").value(100));
	}

	@Test
	void resetsSimulationData() throws Exception {
		mockMvc.perform(post("/api/spotter/simulation/next")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/spotter/simulation/reset")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.appliedEvents").value(0))
				.andExpect(jsonPath("$.summary.totalSpaces").value(100));

		mockMvc.perform(get("/api/spotter/events"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void capsStoredEventsAtOneHundred() throws Exception {
		mockMvc.perform(post("/api/spotter/simulation/reset")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/spotter/simulation/run")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventCount\":105,\"publishEvents\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.appliedEvents").value(105));

		mockMvc.perform(get("/api/spotter/events"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(100)));
	}
}
