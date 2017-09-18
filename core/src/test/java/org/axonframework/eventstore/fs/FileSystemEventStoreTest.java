/*
 * Copyright (c) 2011. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.eventstore.fs;

import org.axonframework.domain.AggregateIdentifier;
import org.axonframework.domain.DomainEvent;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.domain.SimpleDomainEventStream;
import org.axonframework.domain.StubDomainEvent;
import org.axonframework.domain.UUIDAggregateIdentifier;
import org.axonframework.eventstore.EventStoreException;
import org.axonframework.eventstore.XStreamEventSerializer;
import org.junit.*;
import org.mockito.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class FileSystemEventStoreTest {

    private FileSystemEventStore eventStore;
    private AggregateIdentifier aggregateIdentifier;

    @Before
    public void setUp() {
        eventStore = new FileSystemEventStore(new XStreamEventSerializer());
        eventStore.setBaseDir(new File("target/"));

        aggregateIdentifier = new UUIDAggregateIdentifier();
    }

    @Test
    public void testSaveStreamAndReadBackIn() {
        StubDomainEvent event1 = new StubDomainEvent(aggregateIdentifier, 0);
        StubDomainEvent event2 = new StubDomainEvent(aggregateIdentifier, 1);
        StubDomainEvent event3 = new StubDomainEvent(aggregateIdentifier, 2);
        DomainEventStream stream = new SimpleDomainEventStream(event1, event2, event3);
        eventStore.appendEvents("test", stream);

        DomainEventStream eventStream = eventStore.readEvents("test", aggregateIdentifier);
        List<DomainEvent> domainEvents = new ArrayList<DomainEvent>();
        while (eventStream.hasNext()) {
            domainEvents.add(eventStream.next());
        }
        assertEquals(event1, domainEvents.get(0));
        assertEquals(event2, domainEvents.get(1));
        assertEquals(event3, domainEvents.get(2));
    }

    @Test
    // Issue #25: XStreamFileSystemEventStore fails when event data contains newline character
    public void testSaveStreamAndReadBackIn_NewLineInEvent() {
        AggregateIdentifier aggregateId = new UUIDAggregateIdentifier();
        String description = "This is a description with a \n newline character and weird chars éçè\u6324.";
        StringBuilder stringBuilder = new StringBuilder(description);
        for (int i = 0; i < 100; i++) {
            stringBuilder.append(
                    "Some more text to make this event really long. It should not be a problem for the event serializer.");
        }
        description = stringBuilder.toString();
        MyStubDomainEvent event1 = new MyStubDomainEvent(aggregateId, 0, description);
        StubDomainEvent event2 = new StubDomainEvent(aggregateId, 1);
        DomainEventStream stream = new SimpleDomainEventStream(event1, event2);
        eventStore.appendEvents("test", stream);

        DomainEventStream eventStream = eventStore.readEvents("test", aggregateId);
        List<DomainEvent> domainEvents = new ArrayList<DomainEvent>();
        while (eventStream.hasNext()) {
            domainEvents.add(eventStream.next());
        }
        MyStubDomainEvent actualEvent1 = (MyStubDomainEvent) domainEvents.get(0);
        assertEquals(event1, actualEvent1);
        assertEquals(description, actualEvent1.getDescription());
        assertEquals(event2, domainEvents.get(1));
    }

    @Test
    public void testRead_FileNotReadable() throws IOException {
        EventFileResolver mockEventFileResolver = mock(EventFileResolver.class);
        InputStream mockInputStream = mock(InputStream.class);
        when(mockEventFileResolver.eventFileExists(isA(String.class), isA(AggregateIdentifier.class))).thenReturn(true);
        when(mockEventFileResolver.openEventFileForReading(isA(String.class), isA(AggregateIdentifier.class)))
                .thenReturn(mockInputStream);
        IOException exception = new IOException("Mock Exception");
        when(mockInputStream.read()).thenThrow(exception);
        when(mockInputStream.read(Matchers.<byte[]>any())).thenThrow(exception);
        when(mockInputStream.read(Matchers.<byte[]>any(), anyInt(), anyInt())).thenThrow(exception);
        eventStore.setEventFileResolver(mockEventFileResolver);

        try {
            eventStore.readEvents("test", new UUIDAggregateIdentifier());
            fail("Expected an exception");
        } catch (EventStoreException e) {
            assertSame(exception, e.getCause());
        }
    }

    @Test
    public void testWrite_FileDoesNotExist() throws IOException {
        AggregateIdentifier aggregateId = new UUIDAggregateIdentifier();
        IOException exception = new IOException("Mock");
        EventFileResolver mockEventFileResolver = mock(EventFileResolver.class);
        when(mockEventFileResolver.openEventFileForWriting(isA(String.class), isA(AggregateIdentifier.class)))
                .thenThrow(exception);
        eventStore.setEventFileResolver(mockEventFileResolver);

        StubDomainEvent event1 = new StubDomainEvent(aggregateId, 0);
        StubDomainEvent event2 = new StubDomainEvent(aggregateId, 1);
        StubDomainEvent event3 = new StubDomainEvent(aggregateId, 2);
        DomainEventStream stream = new SimpleDomainEventStream(event1, event2, event3);

        try {
            eventStore.appendEvents("test", stream);
            fail("Expected an exception");
        } catch (EventStoreException e) {
            assertEquals(exception, e.getCause());
        }
    }

    @Test
    public void testAppendSnapShot() {
        AtomicInteger counter = new AtomicInteger(0);

        writeEvents(counter, 5);
        eventStore.appendSnapshotEvent("snapshotting", new StubDomainEvent(aggregateIdentifier, 4));
        writeEvents(counter, 5);
        eventStore.appendSnapshotEvent("snapshotting", new StubDomainEvent(aggregateIdentifier, 9));
        writeEvents(counter, 5);
        eventStore.appendSnapshotEvent("snapshotting", new StubDomainEvent(aggregateIdentifier, 14));
        writeEvents(counter, 2);

        DomainEventStream eventStream = eventStore.readEvents("snapshotting", aggregateIdentifier);
        List<DomainEvent> actualEvents = new ArrayList<DomainEvent>();
        while (eventStream.hasNext()) {
            actualEvents.add(eventStream.next());
        }
        assertEquals(new Long(14), actualEvents.get(0).getSequenceNumber());
        assertEquals(3, actualEvents.size());
    }

    private void writeEvents(AtomicInteger counter, int numberOfEvents) {
        List<DomainEvent> events = new ArrayList<DomainEvent>();
        for (int t = 0; t < numberOfEvents; t++) {
            events.add(new StubDomainEvent(aggregateIdentifier, counter.getAndIncrement()));
        }
        eventStore.appendEvents("snapshotting", new SimpleDomainEventStream(events));
    }

    public static class MyStubDomainEvent extends StubDomainEvent {

        private static final long serialVersionUID = -7959231436742664073L;
        private final String description;

        public MyStubDomainEvent(AggregateIdentifier aggregateId, int sequenceNumber,
                                 String description) {
            super(aggregateId, sequenceNumber);
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
