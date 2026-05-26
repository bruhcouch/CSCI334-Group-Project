package com.example.spotter.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spotter.dto.event.SpaceUpdatedEvent;
import com.example.spotter.dto.request.DetectionRequest;
import com.example.spotter.dto.request.SimulationRequest;
import com.example.spotter.dto.request.UpdateRequest;
import com.example.spotter.dto.response.DetectionEventResponse;
import com.example.spotter.dto.response.SimulationEventResponse;
import com.example.spotter.dto.response.SimulationRunResponse;
import com.example.spotter.dto.response.SpaceResponse;
import com.example.spotter.dto.response.SpotterSummaryResponse;
import com.example.spotter.dto.response.ZoneSummaryResponse;
import com.example.spotter.model.DetectionEvent;
import com.example.spotter.model.SimulationFeedRecord;
import com.example.spotter.model.Space;
import com.example.spotter.repository.DetectionEventRepository;
import com.example.spotter.repository.SpaceRepository;
import com.example.spotter.service.dto.event.SpaceCreatedEvent;

@Service
public class SpotterService {
	private final SpaceRepository _repository;
	private SpaceProducerService spaceEventProducer;
	private final DetectionEventRepository detectionEventRepository;
	private final SimulationFeedService simulationFeedService;
	private final SpotterDatasetService datasetService;
	
	public SpotterService(
			SpaceRepository repository,
			SpaceProducerService spaceEventProducer,
			DetectionEventRepository detectionEventRepository,
			SimulationFeedService simulationFeedService,
			SpotterDatasetService datasetService) {
		_repository = repository;
		this.spaceEventProducer = spaceEventProducer;
		this.detectionEventRepository = detectionEventRepository;
		this.simulationFeedService = simulationFeedService;
		this.datasetService = datasetService;
	}

	public List<SpaceResponse> getSpaces(String lotName, String zone, Boolean occupied, Boolean disabilityPermitRequired) {
		return _repository.findAll().stream()
				.filter(space -> lotName == null || space.getLotName().equalsIgnoreCase(lotName))
				.filter(space -> zone == null || space.getZone().equalsIgnoreCase(zone))
				.filter(space -> occupied == null || space.isOccupied() == occupied)
				.filter(space -> disabilityPermitRequired == null
						|| space.isDisabilityPermitRequired() == disabilityPermitRequired)
				.sorted(Comparator.comparing(Space::getLotName)
						.thenComparing(Space::getZone)
						.thenComparing(Space::getBayNumber))
				.map(SpaceResponse::new)
				.toList();
	}

	public List<String> getLots() {
		return _repository.findAll().stream()
				.map(Space::getLotName)
				.distinct()
				.sorted()
				.toList();
	}

	public List<ZoneSummaryResponse> getZones(String lotName) {
		return getSummary(lotName, null).getZones();
	}
	
    public Optional<Boolean> isOccupied(int spaceId) {
    	return _repository.findById((long) spaceId).map((space) -> space.isOccupied());
    }
    
    public void handleSensorActivation(int spaceId) {
    	_repository.findById((long) spaceId).ifPresent((space) -> {
			DetectionRequest request = new DetectionRequest();
			request.setOccupied(!space.isOccupied());
			request.setConfidence(1.0);
			request.setSource("sensor-toggle");
			recordDetection(space.getId(), request, true);
		});
    }
    
    public SpaceResponse getSpace(Long spaceId) {
    	Space ret = _repository.findById(spaceId).orElseThrow(() -> new NoSuchElementException("Space not found"));
    	
    	return new SpaceResponse(ret);
    }

	public SpaceResponse getSpaceBySensorId(String sensorId) {
		Space ret = _repository.findBySensorId(sensorId).orElseThrow(() -> new NoSuchElementException("Space not found"));
		return new SpaceResponse(ret);
	}
    
	@Transactional
    public SpaceResponse update(UpdateRequest request, Long spaceId) {
    	Space ret = _repository.findById(spaceId).orElseThrow(() -> new NoSuchElementException("Space not found"));
    	
		if (request.getLotName() != null) {
			ret.setLotName(request.getLotName());
		}
		if (request.getZone() != null) {
			ret.setZone(request.getZone());
		}
		if (request.getBayNumber() != null) {
			ret.setBayNumber(request.getBayNumber());
		}
		if (request.getSensorId() != null) {
			ret.setSensorId(request.getSensorId());
		}
    	if (request.getMaxParkingMinutes() != null) {
    		ret.setMaxParkingMinutes(request.getMaxParkingMinutes());
    	}
    	if (request.getDisabilityPermitRequired() != null) {
    		ret.setDisabilityPermitRequired(request.getDisabilityPermitRequired());
    	}
    	if (request.getIsOccupied() != null) {
    		ret.applyDetection(
					request.getIsOccupied(),
					request.getConfidence() == null ? ret.getConfidence() : request.getConfidence(),
					request.getSource() == null ? "manual-update" : request.getSource(),
					Instant.now());
    	}
    	
    	Space saved = _repository.save(ret);
    	spaceEventProducer.publishSpaceUpdatedEvent(new SpaceUpdatedEvent(saved));
		return new SpaceResponse(saved);
    }

	@Transactional
	public DetectionEventResponse recordDetection(Long spaceId, DetectionRequest request, boolean publishEvent) {
		Space space = _repository.findById(spaceId).orElseThrow(() -> new NoSuchElementException("Space not found"));
		return applyDetection(
				space,
				request.getOccupied(),
				request.getConfidence() == null ? 1.0 : request.getConfidence(),
				request.getSource() == null ? "manual" : request.getSource(),
				request.getObservedAt() == null ? Instant.now() : request.getObservedAt(),
				publishEvent);
	}

	@Transactional
	public DetectionEventResponse recordSensorDetection(String sensorId, DetectionRequest request, boolean publishEvent) {
		Space space = _repository.findBySensorId(sensorId).orElseThrow(() -> new NoSuchElementException("Space not found"));
		return applyDetection(
				space,
				request.getOccupied(),
				request.getConfidence() == null ? 1.0 : request.getConfidence(),
				request.getSource() == null ? "manual" : request.getSource(),
				request.getObservedAt() == null ? Instant.now() : request.getObservedAt(),
				publishEvent);
	}

	@Transactional
	public SimulationRunResponse runSimulation(SimulationRequest request) {
		int eventCount = request.getEventCount() == null ? 1 : request.getEventCount();
		boolean publishEvents = request.getPublishEvents() == null || request.getPublishEvents();
		List<SimulationEventResponse> appliedEvents = simulationFeedService.nextEvents(eventCount).stream()
				.map(record -> applySimulationRecord(record, publishEvents))
				.toList();
		return new SimulationRunResponse(
				appliedEvents.size(),
				simulationFeedService.getFeedSize(),
				simulationFeedService.getNextFeedIndex(),
				appliedEvents,
				getSummary(null, null));
	}

	@Transactional
	public SimulationRunResponse resetSimulation() {
		detectionEventRepository.deleteAllInBatch();
		detectionEventRepository.flush();
		_repository.deleteAllInBatch();
		_repository.flush();
		List<Space> spaces = _repository.saveAll(datasetService.loadSpaces());
		spaces.stream()
				.map(SpaceCreatedEvent::new)
				.forEach(spaceEventProducer::publishSpaceCreatedEvent);
		simulationFeedService.reset();
		return new SimulationRunResponse(
				0,
				simulationFeedService.getFeedSize(),
				simulationFeedService.getNextFeedIndex(),
				List.of(),
				getSummary(null, null));
	}

	public SpotterSummaryResponse getSummary(String lotName, String zone) {
		List<Space> spaces = _repository.findAll().stream()
				.filter(space -> lotName == null || space.getLotName().equalsIgnoreCase(lotName))
				.filter(space -> zone == null || space.getZone().equalsIgnoreCase(zone))
				.toList();

		int totalSpaces = spaces.size();
		int occupiedSpaces = (int) spaces.stream().filter(Space::isOccupied).count();
		int disabilityPermitSpaces = (int) spaces.stream().filter(Space::isDisabilityPermitRequired).count();
		int availableDisabilityPermitSpaces = (int) spaces.stream()
				.filter(space -> space.isDisabilityPermitRequired() && !space.isOccupied())
				.count();
		List<ZoneSummaryResponse> zones = buildZoneSummaries(spaces);
		return new SpotterSummaryResponse(
				totalSpaces,
				occupiedSpaces,
				totalSpaces - occupiedSpaces,
				disabilityPermitSpaces,
				availableDisabilityPermitSpaces,
				toRate(occupiedSpaces, totalSpaces),
				zones);
	}

	public List<DetectionEventResponse> getRecentEvents() {
		return detectionEventRepository.findAllByOrderByDetectedAtDesc().stream()
				.limit(100)
				.map(DetectionEventResponse::new)
				.toList();
	}

	private SimulationEventResponse applySimulationRecord(SimulationFeedRecord record, boolean publishEvent) {
		Space space = _repository.findBySensorId(record.getSensorId())
				.orElseThrow(() -> new NoSuchElementException("Space not found for sensor " + record.getSensorId()));
		DetectionEventResponse event = applyDetection(
				space,
				record.isOccupied(),
				record.getConfidence(),
				record.getSource(),
				Instant.now(),
				publishEvent);
		SpaceResponse response = getSpace(space.getId());
		return new SimulationEventResponse(record.getSequenceNumber(), response, event);
	}

	private DetectionEventResponse applyDetection(
			Space space,
			boolean occupied,
			double confidence,
			String source,
			Instant detectedAt,
			boolean publishEvent) {
		boolean previousOccupied = space.isOccupied();
		space.applyDetection(occupied, confidence, source, detectedAt);
		Space savedSpace = _repository.save(space);
		DetectionEvent event = detectionEventRepository.save(new DetectionEvent(
				savedSpace,
				previousOccupied,
				occupied,
				confidence,
				source,
				detectedAt));
		if (publishEvent) {
			spaceEventProducer.publishSpaceUpdatedEvent(new SpaceUpdatedEvent(savedSpace));
		}
		return new DetectionEventResponse(event);
	}

	private List<ZoneSummaryResponse> buildZoneSummaries(List<Space> spaces) {
		Map<String, List<Space>> spacesByZone = spaces.stream()
				.collect(Collectors.groupingBy(
						space -> space.getLotName() + "|" + space.getZone(),
						LinkedHashMap::new,
						Collectors.toList()));

		return spacesByZone.entrySet().stream()
				.map(entry -> {
					List<Space> zoneSpaces = entry.getValue();
					Space first = zoneSpaces.get(0);
					int totalSpaces = zoneSpaces.size();
					int occupiedSpaces = (int) zoneSpaces.stream().filter(Space::isOccupied).count();
					return new ZoneSummaryResponse(
							first.getLotName(),
							first.getZone(),
							totalSpaces,
							occupiedSpaces,
							totalSpaces - occupiedSpaces,
							toRate(occupiedSpaces, totalSpaces));
				})
				.sorted(Comparator.comparing(ZoneSummaryResponse::getLotName)
						.thenComparing(ZoneSummaryResponse::getZone))
				.toList();
	}

	private double toRate(int occupiedSpaces, int totalSpaces) {
		if (totalSpaces == 0) {
			return 0;
		}
		return Math.round(((double) occupiedSpaces / totalSpaces) * 10000.0) / 100.0;
	}
}
