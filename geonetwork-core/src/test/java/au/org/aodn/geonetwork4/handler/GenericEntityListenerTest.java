package au.org.aodn.geonetwork4.handler;

import org.fao.geonet.domain.Metadata;
import org.fao.geonet.entitylistener.PersistentEventType;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GenericEntityListenerTest {
    /**
     * This test is to check whether update and delete to indexer is called correctly.
     */
    @Test
    public void verifyUpdateDeleteBehavior() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        GenericEntityListener listener = new GenericEntityListener();
        RestTemplate template = Mockito.mock(RestTemplate.class);

        // Set of test only, a mock to count what have been called
        listener.indexUrl = "http://localhost/api/v1/indexer/index/{uuid}";
        listener.apiKey = "test-key";
        listener.restTemplate = template;

        listener.init();

        // Test data
        Metadata metadata1 = new Metadata();
        metadata1.setUuid("54d0cc03-763c-4393-8796-d79c9979e3f8");

        Metadata metadata2 = new Metadata();
        metadata2.setUuid("c401f091-fed5-4829-bead-f6cecad3d424");

        Metadata metadata3 = new Metadata();
        metadata3.setUuid("499131b5-2df6-45c4-b80e-499355dfcbf8");

        listener.handleEvent(PersistentEventType.PostUpdate, metadata1);
        listener.handleEvent(PersistentEventType.PostUpdate, metadata2);
        listener.handleEvent(PersistentEventType.PostRemove, metadata3);

        // There is a delay before the scheduler starts, so we wait 1 more seconds to make
        // sure the last execution completed.
        latch.await(listener.delayStart + 1, TimeUnit.SECONDS);

        verify(template, times(2))
                .postForEntity(
                        eq("http://localhost/api/v1/indexer/index/{uuid}"),
                        any(),
                        eq(Void.class),
                        anyMap());

        verify(template, times(1))
                .exchange(
                        eq("http://localhost/api/v1/indexer/index/{uuid}"),
                        eq(HttpMethod.DELETE),
                        any(),
                        eq(Void.class),
                        anyMap());

        assertTrue("Update map is empty", listener.updateMap.isEmpty());
        assertTrue("Delete map is empty", listener.deleteMap.isEmpty());

        listener.cleanUp();
    }
    /**
     * Make sure if remote service indexer is not available, we will keep retry and not lost the update.
     * @throws InterruptedException - Should not happen
     */
    @Test
    public void verifyRetryBehavior() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        GenericEntityListener listener = new GenericEntityListener();
        RestTemplate template = Mockito.mock(RestTemplate.class);

        // Throw exception on first call
        when(template.postForEntity(
                eq("http://localhost/api/v1/indexer/index/{uuid}"),
                any(),
                eq(Void.class),
                anyMap())
        )
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
                .thenReturn(ResponseEntity.ok(null));


        when(template.exchange(
                eq("http://localhost/api/v1/indexer/index/{uuid}"),
                eq(HttpMethod.DELETE),
                any(),
                eq(Void.class),
                anyMap())
        )
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
                .thenReturn(ResponseEntity.ok(null));

        // Set of test only, a mock to count what have been called
        listener.indexUrl = "http://localhost/api/v1/indexer/index/{uuid}";
        listener.apiKey = "test-key";
        listener.restTemplate = template;

        listener.init();

        // Test data
        Metadata metadata1 = new Metadata();
        metadata1.setUuid("54d0cc03-763c-4393-8796-d79c9979e3f8");

        Metadata metadata2 = new Metadata();
        metadata2.setUuid("c401f091-fed5-4829-bead-f6cecad3d424");

        listener.handleEvent(PersistentEventType.PostUpdate, metadata1);
        listener.handleEvent(PersistentEventType.PostRemove, metadata2);

        // There is a delay before the scheduler starts, so we wait 1 more seconds to make
        // sure the last execution completed.
        latch.await(listener.delayStart + 1, TimeUnit.SECONDS);
        assertEquals("Map contains uuid", 1, listener.updateMap.size());
        assertEquals("Delete contains uuid", 1, listener.deleteMap.size());

        // Wait more time, this time service ok and processed hence map is clear
        latch.await(listener.delayStart + listener.delayStart , TimeUnit.SECONDS);
        assertEquals("Map not contains uuid", 0, listener.updateMap.size());
        assertEquals("Delete not  contains uuid", 0, listener.deleteMap.size());
    }
}
