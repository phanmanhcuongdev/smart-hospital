package vn.edu.ptit.smarthms.integration.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import vn.edu.ptit.smarthms.hr.service.SchIntegrationDispatcher.SchedulerClient;

public class MockSchedulerClient implements SchedulerClient {

    private boolean simulateTimeout = false;
    private final List<String> deliveredKeys = Collections.synchronizedList(new ArrayList<>());

    public void setSimulateTimeout(boolean simulateTimeout) {
        this.simulateTimeout = simulateTimeout;
    }

    public List<String> getDeliveredKeys() {
        return Collections.unmodifiableList(deliveredKeys);
    }

    @Override
    public void deliverEvent(String eventKey, String payload) throws TimeoutException {
        if (simulateTimeout) {
            throw new TimeoutException("Connection to downstream Scheduler (SCH) timed out after 3000ms");
        }
        deliveredKeys.add(eventKey);
    }

    public void reset() {
        simulateTimeout = false;
        deliveredKeys.clear();
    }
}
