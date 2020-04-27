package com.carolinarollergirls.scoreboard.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.Jam;

public class EventPackageTests {

    private int batchLevel;
    private ScoreBoardListener batchCounter = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent<?> event) {
            synchronized (batchCounter) {
                if (event.getProperty() == ScoreBoardEventProviderImpl.BATCH_START) {
                    batchLevel++;
                } else if (event.getProperty() == ScoreBoardEventProviderImpl.BATCH_END) {
                    batchLevel--;
                }
            }
        }
    };

    private Queue<ScoreBoardEvent<?>> collectedEvents;
    public ScoreBoardListener listener = new ScoreBoardListener() {

        @Override
        public void scoreBoardChange(ScoreBoardEvent<?> event) {
            synchronized (collectedEvents) {
                collectedEvents.add(event);
            }
        }
    };

    private TestScoreBoardEventProvider root;

    @Before
    public void setUp() throws Exception {
        root = new TestScoreBoardEventProviderImpl();
        collectedEvents = new LinkedList<>();
        root.addScoreBoardListener(batchCounter);
    }

    @After
    public void tearDown() throws Exception {
        // Check all started batches were ended.
        assertEquals(0, batchLevel);
    }

    @Test
    public void testSet() {
        root.addScoreBoardListener(
                new ConditionalScoreBoardListener<>(root, TestScoreBoardEventProvider.INT, listener));

        assertEquals(0, (int) root.get(TestScoreBoardEventProvider.INT));
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).valuesChanged);
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).valuesRecalculated);

        root.set(TestScoreBoardEventProvider.INT, 3);
        assertEquals(3, (int) root.get(TestScoreBoardEventProvider.INT));
        assertEquals(1, ((TestScoreBoardEventProviderImpl) root).valuesChanged);
        assertEquals(2, ((TestScoreBoardEventProviderImpl) root).valuesRecalculated);
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertEquals(3, event.getValue());
        assertEquals(0, event.getPreviousValue());

        root.set(TestScoreBoardEventProvider.INT, 3);
        assertEquals(3, (int) root.get(TestScoreBoardEventProvider.INT));
        assertEquals(1, ((TestScoreBoardEventProviderImpl) root).valuesChanged);
        assertEquals(3, ((TestScoreBoardEventProviderImpl) root).valuesRecalculated);
        assertEquals(0, collectedEvents.size());
    }

    @Test
    public void testAddRemoveUnordered() {
        root.addScoreBoardListener(
                new ConditionalScoreBoardListener<>(root, TestScoreBoardEventProvider.MULTIPLE, listener));
        TestScoreBoardEventProvider child1 = new TestScoreBoardEventProviderImpl(root, UUID.randomUUID().toString(),
                TestScoreBoardEventProvider.MULTIPLE);
        TestScoreBoardEventProvider child2 = new TestScoreBoardEventProviderImpl(root, UUID.randomUUID().toString(),
                TestScoreBoardEventProvider.MULTIPLE);

        assertEquals(0, root.numberOf(TestScoreBoardEventProvider.MULTIPLE));
        assertEquals(1, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertTrue(root.add(TestScoreBoardEventProvider.MULTIPLE, child1));
        assertEquals(1, root.numberOf(TestScoreBoardEventProvider.MULTIPLE));
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertEquals(child1, event.getValue());
        assertFalse(event.isRemove());
        assertEquals(2, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(child1, root.get(TestScoreBoardEventProvider.MULTIPLE, child1.getId()));
        assertEquals(null, root.get(TestScoreBoardEventProvider.MULTIPLE, child2.getId()));

        assertTrue(root.add(TestScoreBoardEventProvider.MULTIPLE, child2));
        assertEquals(2, root.numberOf(TestScoreBoardEventProvider.MULTIPLE));
        assertEquals(1, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals(child2, event.getValue());
        assertFalse(event.isRemove());
        assertEquals(3, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(child1, root.get(TestScoreBoardEventProvider.MULTIPLE, child1.getId()));
        assertEquals(child2, root.get(TestScoreBoardEventProvider.MULTIPLE, child2.getId()));

        assertFalse(root.add(TestScoreBoardEventProvider.MULTIPLE, child2));
        assertEquals(2, root.numberOf(TestScoreBoardEventProvider.MULTIPLE));
        assertEquals(0, collectedEvents.size());
        assertEquals(3, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(child1, root.get(TestScoreBoardEventProvider.MULTIPLE, child1.getId()));
        assertEquals(child2, root.get(TestScoreBoardEventProvider.MULTIPLE, child2.getId()));

        assertTrue(root.remove(TestScoreBoardEventProvider.MULTIPLE, child1));
        assertEquals(1, root.numberOf(TestScoreBoardEventProvider.MULTIPLE));
        assertEquals(1, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals(child1, event.getValue());
        assertTrue(event.isRemove());
        assertEquals(3, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(1, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(null, root.get(TestScoreBoardEventProvider.MULTIPLE, child1.getId()));
        assertEquals(child2, root.get(TestScoreBoardEventProvider.MULTIPLE, child2.getId()));

        assertFalse(root.remove(TestScoreBoardEventProvider.MULTIPLE, child1));
        assertEquals(1, root.numberOf(TestScoreBoardEventProvider.MULTIPLE));
        assertEquals(0, collectedEvents.size());
        assertEquals(3, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(1, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(null, root.get(TestScoreBoardEventProvider.MULTIPLE, child1.getId()));
        assertEquals(child2, root.get(TestScoreBoardEventProvider.MULTIPLE, child2.getId()));

        assertTrue(root.add(TestScoreBoardEventProvider.MULTIPLE, child1));
        assertEquals(2, root.numberOf(TestScoreBoardEventProvider.MULTIPLE));
        assertEquals(1, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals(child1, event.getValue());
        assertFalse(event.isRemove());
        assertEquals(4, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(1, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(child1, root.get(TestScoreBoardEventProvider.MULTIPLE, child1.getId()));
        assertEquals(child2, root.get(TestScoreBoardEventProvider.MULTIPLE, child2.getId()));

        child2.delete();
        assertEquals(1, root.numberOf(TestScoreBoardEventProvider.MULTIPLE));
        assertEquals(1, collectedEvents.size());
        assertEquals(4, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(2, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(child1, root.get(TestScoreBoardEventProvider.MULTIPLE, child1.getId()));
        assertEquals(null, root.get(TestScoreBoardEventProvider.MULTIPLE, child2.getId()));
    }

    @Test
    public void testSingleton() {
        root.addScoreBoardListener(
                new ConditionalScoreBoardListener<>(root, TestScoreBoardEventProvider.SINGLETON, listener));
        TestScoreBoardEventProvider singleton = root.get(TestScoreBoardEventProvider.SINGLETON, "");
        TestScoreBoardEventProvider s1 = new TestScoreBoardEventProviderImpl(root, UUID.randomUUID().toString(),
                TestScoreBoardEventProvider.SINGLETON);

        assertEquals(1, root.numberOf(TestScoreBoardEventProvider.SINGLETON));
        assertEquals(1, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertFalse(root.add(TestScoreBoardEventProvider.SINGLETON, s1));
        assertEquals(1, root.numberOf(TestScoreBoardEventProvider.SINGLETON));
        assertEquals(0, collectedEvents.size());
        assertEquals(1, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(singleton, root.get(TestScoreBoardEventProvider.SINGLETON, ""));

        assertFalse(root.remove(TestScoreBoardEventProvider.SINGLETON, singleton));
        assertEquals(1, root.numberOf(TestScoreBoardEventProvider.SINGLETON));
        assertEquals(0, collectedEvents.size());
        assertEquals(1, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(singleton, root.get(TestScoreBoardEventProvider.SINGLETON, ""));
    }

    @Test
    public void testAddRemoveNumbered() {
        root.addScoreBoardListener(
                new ConditionalScoreBoardListener<>(root, TestScoreBoardEventProvider.NUMBERED, listener));
        TestNumberedScoreBoardEventProvider child1 = new TestNumberedScoreBoardEventProviderImpl(root, 1);

        assertEquals(1, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        TestParentOrderedScoreBoardEventProvider subchild1 = new TestParentOrderedScoreBoardEventProviderImpl(child1,
                "A");
        TestParentOrderedScoreBoardEventProvider subsubchild1 = new TestParentOrderedScoreBoardEventProviderImpl(
                subchild1, "1");
        assertTrue(subchild1.add(TestParentOrderedScoreBoardEventProvider.CO_ORDERED, subsubchild1));
        assertTrue(child1.add(TestNumberedScoreBoardEventProvider.CO_ORDERED, subchild1));
        assertTrue(root.add(TestScoreBoardEventProvider.NUMBERED, child1));
        assertEquals(1, root.numberOf(TestScoreBoardEventProvider.NUMBERED));
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertEquals(child1, event.getValue());
        assertFalse(event.isRemove());
        assertEquals(2, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(1, child1.getNumber());
        assertEquals(1, subchild1.getNumber());
        assertEquals(1, subsubchild1.getNumber());
        assertEquals(child1, root.get(TestScoreBoardEventProvider.NUMBERED, 1));
        assertEquals(subchild1, child1.get(TestNumberedScoreBoardEventProvider.CO_ORDERED, "A"));
        assertEquals(subsubchild1, subchild1.get(TestParentOrderedScoreBoardEventProvider.CO_ORDERED, "1"));
        assertNull(child1.getNext());
        assertNull(child1.getPrevious());
        assertNull(subchild1.getNext());
        assertNull(subchild1.getPrevious());
        assertNull(subsubchild1.getNext());
        assertNull(subsubchild1.getPrevious());

        TestNumberedScoreBoardEventProvider child2 = new TestNumberedScoreBoardEventProviderImpl(root, 2);
        TestParentOrderedScoreBoardEventProvider subchild2 = new TestParentOrderedScoreBoardEventProviderImpl(child2,
                "A");
        TestParentOrderedScoreBoardEventProvider subsubchild2 = new TestParentOrderedScoreBoardEventProviderImpl(
                subchild2, "1");
        assertTrue(root.add(TestScoreBoardEventProvider.NUMBERED, child2));
        assertTrue(child2.add(TestNumberedScoreBoardEventProvider.CO_ORDERED, subchild2));
        assertTrue(subchild2.add(TestParentOrderedScoreBoardEventProvider.CO_ORDERED, subsubchild2));
        assertEquals(2, root.numberOf(TestScoreBoardEventProvider.NUMBERED));
        assertEquals(1, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals(child2, event.getValue());
        assertFalse(event.isRemove());
        assertEquals(3, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(1, child1.getNumber());
        assertEquals(1, subchild1.getNumber());
        assertEquals(1, subsubchild1.getNumber());
        assertEquals(2, child2.getNumber());
        assertEquals(2, subchild2.getNumber());
        assertEquals(2, subsubchild2.getNumber());
        assertEquals(child1, root.get(TestScoreBoardEventProvider.NUMBERED, 1));
        assertEquals(child2, root.get(TestScoreBoardEventProvider.NUMBERED, 2));
        assertEquals(subchild2, child2.get(TestNumberedScoreBoardEventProvider.CO_ORDERED, "A"));
        assertEquals(subsubchild2, subchild2.get(TestParentOrderedScoreBoardEventProvider.CO_ORDERED, "1"));
        assertEquals(child2, child1.getNext());
        assertNull(child1.getPrevious());
        assertEquals(subchild2, subchild1.getNext());
        assertNull(subchild1.getPrevious());
        assertEquals(subsubchild2, subsubchild1.getNext());
        assertNull(subsubchild1.getPrevious());
        assertNull(child2.getNext());
        assertEquals(child1, child2.getPrevious());
        assertNull(subchild2.getNext());
        assertEquals(subchild1, subchild2.getPrevious());
        assertNull(subsubchild2.getNext());
        assertEquals(subsubchild1, subsubchild2.getPrevious());

        TestNumberedScoreBoardEventProvider child5 = new TestNumberedScoreBoardEventProviderImpl(root, 4);
        TestParentOrderedScoreBoardEventProvider subchild5 = new TestParentOrderedScoreBoardEventProviderImpl(child5,
                "A");
        TestParentOrderedScoreBoardEventProvider subsubchild5 = new TestParentOrderedScoreBoardEventProviderImpl(
                subchild5, "1");
        assertTrue(subchild5.add(TestParentOrderedScoreBoardEventProvider.CO_ORDERED, subsubchild5));
        assertTrue(child5.add(TestNumberedScoreBoardEventProvider.CO_ORDERED, subchild5));
        assertTrue(root.add(TestScoreBoardEventProvider.NUMBERED, child5));
        assertEquals(3, root.numberOf(TestScoreBoardEventProvider.NUMBERED));
        assertEquals(1, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals(child5, event.getValue());
        assertFalse(event.isRemove());
        assertEquals(4, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(1, child1.getNumber());
        assertEquals(1, subchild1.getNumber());
        assertEquals(1, subsubchild1.getNumber());
        assertEquals(2, child2.getNumber());
        assertEquals(2, subchild2.getNumber());
        assertEquals(2, subsubchild2.getNumber());
        assertEquals(4, child5.getNumber());
        assertEquals(4, subchild5.getNumber());
        assertEquals(4, subsubchild5.getNumber());
        assertEquals(child1, root.get(TestScoreBoardEventProvider.NUMBERED, 1));
        assertEquals(child2, root.get(TestScoreBoardEventProvider.NUMBERED, 2));
        assertNull(root.get(TestScoreBoardEventProvider.NUMBERED, 3));
        assertEquals(child5, root.get(TestScoreBoardEventProvider.NUMBERED, 4));
        assertEquals(subchild5, child5.get(TestNumberedScoreBoardEventProvider.CO_ORDERED, "A"));
        assertEquals(subsubchild5, subchild5.get(TestParentOrderedScoreBoardEventProvider.CO_ORDERED, "1"));
        assertEquals(child2, child1.getNext());
        assertNull(child1.getPrevious());
        assertEquals(subchild2, subchild1.getNext());
        assertNull(subchild1.getPrevious());
        assertEquals(subsubchild2, subsubchild1.getNext());
        assertNull(subsubchild1.getPrevious());
        assertEquals(child5, child2.getNext());
        assertEquals(child1, child2.getPrevious());
        assertEquals(subchild5, subchild2.getNext());
        assertEquals(subchild1, subchild2.getPrevious());
        assertEquals(subsubchild5, subsubchild2.getNext());
        assertEquals(subsubchild1, subsubchild2.getPrevious());
        assertNull(child5.getNext());
        assertEquals(child2, child5.getPrevious());
        assertNull(subchild5.getNext());
        assertEquals(subchild2, subchild5.getPrevious());
        assertNull(subsubchild5.getNext());
        assertEquals(subsubchild2, subsubchild5.getPrevious());

        TestNumberedScoreBoardEventProvider child3 = new TestNumberedScoreBoardEventProviderImpl(root, 3);
        TestParentOrderedScoreBoardEventProvider subchild3 = new TestParentOrderedScoreBoardEventProviderImpl(child3,
                "A");
        TestParentOrderedScoreBoardEventProvider subsubchild3 = new TestParentOrderedScoreBoardEventProviderImpl(
                subchild3, "1");
        assertTrue(subchild3.add(TestParentOrderedScoreBoardEventProvider.CO_ORDERED, subsubchild3));
        assertTrue(child3.add(TestNumberedScoreBoardEventProvider.CO_ORDERED, subchild3));
        assertTrue(root.add(TestScoreBoardEventProvider.NUMBERED, child3));
        assertEquals(4, root.numberOf(TestScoreBoardEventProvider.NUMBERED));
        assertEquals(1, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals(child3, event.getValue());
        assertFalse(event.isRemove());
        assertEquals(5, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(1, child1.getNumber());
        assertEquals(1, subchild1.getNumber());
        assertEquals(1, subsubchild1.getNumber());
        assertEquals(2, child2.getNumber());
        assertEquals(2, subchild2.getNumber());
        assertEquals(2, subsubchild2.getNumber());
        assertEquals(3, child3.getNumber());
        assertEquals(3, subchild3.getNumber());
        assertEquals(3, subsubchild3.getNumber());
        assertEquals(4, child5.getNumber());
        assertEquals(4, subchild5.getNumber());
        assertEquals(4, subsubchild5.getNumber());
        assertEquals(child1, root.get(TestScoreBoardEventProvider.NUMBERED, 1));
        assertEquals(child2, root.get(TestScoreBoardEventProvider.NUMBERED, 2));
        assertEquals(child3, root.get(TestScoreBoardEventProvider.NUMBERED, 3));
        assertEquals(child5, root.get(TestScoreBoardEventProvider.NUMBERED, 4));
        assertEquals(child2, child1.getNext());
        assertNull(child1.getPrevious());
        assertEquals(subchild2, subchild1.getNext());
        assertNull(subchild1.getPrevious());
        assertEquals(subsubchild2, subsubchild1.getNext());
        assertNull(subsubchild1.getPrevious());
        assertEquals(child3, child2.getNext());
        assertEquals(child1, child2.getPrevious());
        assertEquals(subchild3, subchild2.getNext());
        assertEquals(subchild1, subchild2.getPrevious());
        assertEquals(subsubchild3, subsubchild2.getNext());
        assertEquals(subsubchild1, subsubchild2.getPrevious());
        assertEquals(child5, child3.getNext());
        assertEquals(child2, child3.getPrevious());
        assertEquals(subchild5, subchild3.getNext());
        assertEquals(subchild2, subchild3.getPrevious());
        assertEquals(subsubchild5, subsubchild3.getNext());
        assertEquals(subsubchild2, subsubchild3.getPrevious());
        assertNull(child5.getNext());
        assertEquals(child3, child5.getPrevious());
        assertNull(subchild5.getNext());
        assertEquals(subchild3, subchild5.getPrevious());
        assertNull(subsubchild5.getNext());
        assertEquals(subsubchild3, subsubchild5.getPrevious());

        TestNumberedScoreBoardEventProvider child4 = new TestNumberedScoreBoardEventProviderImpl(root, 4);
        TestParentOrderedScoreBoardEventProvider subchild4 = new TestParentOrderedScoreBoardEventProviderImpl(child4,
                "A");
        TestParentOrderedScoreBoardEventProvider subsubchild4 = new TestParentOrderedScoreBoardEventProviderImpl(
                subchild4, "1");
        assertTrue(subchild4.add(TestParentOrderedScoreBoardEventProvider.CO_ORDERED, subsubchild4));
        assertTrue(child4.add(TestNumberedScoreBoardEventProvider.CO_ORDERED, subchild4));
        assertTrue(root.add(TestScoreBoardEventProvider.NUMBERED, child4));
        assertEquals(5, root.numberOf(TestScoreBoardEventProvider.NUMBERED));
        assertEquals(3, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals(child5, event.getValue());
        assertTrue(event.isRemove());
        event = collectedEvents.poll();
        assertEquals(child5, event.getValue());
        assertFalse(event.isRemove());
        event = collectedEvents.poll();
        assertEquals(child4, event.getValue());
        assertFalse(event.isRemove());
        assertEquals(7, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(1, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(1, child1.getNumber());
        assertEquals(1, subchild1.getNumber());
        assertEquals(1, subsubchild1.getNumber());
        assertEquals(2, child2.getNumber());
        assertEquals(2, subchild2.getNumber());
        assertEquals(2, subsubchild2.getNumber());
        assertEquals(3, child3.getNumber());
        assertEquals(3, subchild3.getNumber());
        assertEquals(3, subsubchild3.getNumber());
        assertEquals(4, child4.getNumber());
        assertEquals(4, subchild4.getNumber());
        assertEquals(4, subsubchild4.getNumber());
        assertEquals(5, child5.getNumber());
        assertEquals(5, subchild5.getNumber());
        assertEquals(5, subsubchild5.getNumber());
        assertEquals(child1, root.get(TestScoreBoardEventProvider.NUMBERED, 1));
        assertEquals(child2, root.get(TestScoreBoardEventProvider.NUMBERED, 2));
        assertEquals(child3, root.get(TestScoreBoardEventProvider.NUMBERED, 3));
        assertEquals(child4, root.get(TestScoreBoardEventProvider.NUMBERED, 4));
        assertEquals(child5, root.get(TestScoreBoardEventProvider.NUMBERED, 5));
        assertEquals(child2, child1.getNext());
        assertNull(child1.getPrevious());
        assertEquals(subchild2, subchild1.getNext());
        assertNull(subchild1.getPrevious());
        assertEquals(subsubchild2, subsubchild1.getNext());
        assertNull(subsubchild1.getPrevious());
        assertEquals(child3, child2.getNext());
        assertEquals(child1, child2.getPrevious());
        assertEquals(subchild3, subchild2.getNext());
        assertEquals(subchild1, subchild2.getPrevious());
        assertEquals(subsubchild3, subsubchild2.getNext());
        assertEquals(subsubchild1, subsubchild2.getPrevious());
        assertEquals(child4, child3.getNext());
        assertEquals(child2, child3.getPrevious());
        assertEquals(subchild4, subchild3.getNext());
        assertEquals(subchild2, subchild3.getPrevious());
        assertEquals(subsubchild4, subsubchild3.getNext());
        assertEquals(subsubchild2, subsubchild3.getPrevious());
        assertEquals(child5, child4.getNext());
        assertEquals(child3, child4.getPrevious());
        assertEquals(subchild5, subchild4.getNext());
        assertEquals(subchild3, subchild4.getPrevious());
        assertEquals(subsubchild5, subsubchild4.getNext());
        assertEquals(subsubchild3, subsubchild4.getPrevious());
        assertNull(child5.getNext());
        assertEquals(child4, child5.getPrevious());
        assertNull(subchild5.getNext());
        assertEquals(subchild4, subchild5.getPrevious());
        assertNull(subsubchild5.getNext());
        assertEquals(subsubchild4, subsubchild5.getPrevious());

        assertFalse(root.add(TestScoreBoardEventProvider.NUMBERED, child3));
        assertEquals(5, root.numberOf(TestScoreBoardEventProvider.NUMBERED));
        assertEquals(0, collectedEvents.size());
        assertEquals(7, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(1, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(1, child1.getNumber());
        assertEquals(1, subchild1.getNumber());
        assertEquals(1, subsubchild1.getNumber());
        assertEquals(2, child2.getNumber());
        assertEquals(2, subchild2.getNumber());
        assertEquals(2, subsubchild2.getNumber());
        assertEquals(3, child3.getNumber());
        assertEquals(3, subchild3.getNumber());
        assertEquals(3, subsubchild3.getNumber());
        assertEquals(4, child4.getNumber());
        assertEquals(4, subchild4.getNumber());
        assertEquals(4, subsubchild4.getNumber());
        assertEquals(5, child5.getNumber());
        assertEquals(5, subchild5.getNumber());
        assertEquals(5, subsubchild5.getNumber());
        assertEquals(child1, root.get(TestScoreBoardEventProvider.NUMBERED, 1));
        assertEquals(child2, root.get(TestScoreBoardEventProvider.NUMBERED, 2));
        assertEquals(child3, root.get(TestScoreBoardEventProvider.NUMBERED, 3));
        assertEquals(child4, root.get(TestScoreBoardEventProvider.NUMBERED, 4));
        assertEquals(child5, root.get(TestScoreBoardEventProvider.NUMBERED, 5));
        assertEquals(child2, child1.getNext());
        assertNull(child1.getPrevious());
        assertEquals(subchild2, subchild1.getNext());
        assertNull(subchild1.getPrevious());
        assertEquals(subsubchild2, subsubchild1.getNext());
        assertNull(subsubchild1.getPrevious());
        assertEquals(child3, child2.getNext());
        assertEquals(child1, child2.getPrevious());
        assertEquals(subchild3, subchild2.getNext());
        assertEquals(subchild1, subchild2.getPrevious());
        assertEquals(subsubchild3, subsubchild2.getNext());
        assertEquals(subsubchild1, subsubchild2.getPrevious());
        assertEquals(child4, child3.getNext());
        assertEquals(child2, child3.getPrevious());
        assertEquals(subchild4, subchild3.getNext());
        assertEquals(subchild2, subchild3.getPrevious());
        assertEquals(subsubchild4, subsubchild3.getNext());
        assertEquals(subsubchild2, subsubchild3.getPrevious());
        assertEquals(child5, child4.getNext());
        assertEquals(child3, child4.getPrevious());
        assertEquals(subchild5, subchild4.getNext());
        assertEquals(subchild3, subchild4.getPrevious());
        assertEquals(subsubchild5, subsubchild4.getNext());
        assertEquals(subsubchild3, subsubchild4.getPrevious());
        assertNull(child5.getNext());
        assertEquals(child4, child5.getPrevious());
        assertNull(subchild5.getNext());
        assertEquals(subchild4, subchild5.getPrevious());
        assertNull(subsubchild5.getNext());
        assertEquals(subsubchild4, subsubchild5.getPrevious());

        child3.delete();
        assertEquals(4, root.numberOf(TestScoreBoardEventProvider.NUMBERED));
        assertEquals(5, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals(child3, event.getValue());
        assertTrue(event.isRemove());
        event = collectedEvents.poll();
        assertEquals(child4, event.getValue());
        event = collectedEvents.poll();
        assertEquals(child5, event.getValue());
        assertTrue(event.isRemove());
        assertTrue(event.isRemove());
        event = collectedEvents.poll();
        assertEquals(child5, event.getValue());
        assertFalse(event.isRemove());
        event = collectedEvents.poll();
        assertEquals(child4, event.getValue());
        assertFalse(event.isRemove());
        assertEquals(9, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(4, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(1, child1.getNumber());
        assertEquals(1, subchild1.getNumber());
        assertEquals(1, subsubchild1.getNumber());
        assertEquals(2, child2.getNumber());
        assertEquals(2, subchild2.getNumber());
        assertEquals(2, subsubchild2.getNumber());
        assertEquals(3, child4.getNumber());
        assertEquals(3, subchild4.getNumber());
        assertEquals(3, subsubchild4.getNumber());
        assertEquals(4, child5.getNumber());
        assertEquals(4, subchild5.getNumber());
        assertEquals(4, subsubchild5.getNumber());
        assertEquals(child1, root.get(TestScoreBoardEventProvider.NUMBERED, 1));
        assertEquals(child2, root.get(TestScoreBoardEventProvider.NUMBERED, 2));
        assertEquals(child4, root.get(TestScoreBoardEventProvider.NUMBERED, 3));
        assertEquals(child5, root.get(TestScoreBoardEventProvider.NUMBERED, 4));
        assertEquals(child2, child1.getNext());
        assertNull(child1.getPrevious());
        assertEquals(subchild2, subchild1.getNext());
        assertNull(subchild1.getPrevious());
        assertEquals(subsubchild2, subsubchild1.getNext());
        assertNull(subsubchild1.getPrevious());
        assertEquals(child4, child2.getNext());
        assertEquals(child1, child2.getPrevious());
        assertEquals(subchild4, subchild2.getNext());
        assertEquals(subchild1, subchild2.getPrevious());
        assertEquals(subsubchild4, subsubchild2.getNext());
        assertEquals(subsubchild1, subsubchild2.getPrevious());
        assertEquals(child5, child4.getNext());
        assertEquals(child2, child4.getPrevious());
        assertEquals(subchild5, subchild4.getNext());
        assertEquals(subchild2, subchild4.getPrevious());
        assertEquals(subsubchild5, subsubchild4.getNext());
        assertEquals(subsubchild2, subsubchild4.getPrevious());
        assertNull(child5.getNext());
        assertEquals(child4, child5.getPrevious());
        assertNull(subchild5.getNext());
        assertEquals(subchild4, subchild5.getPrevious());
        assertNull(subsubchild5.getNext());
        assertEquals(subsubchild4, subsubchild5.getPrevious());

        assertTrue(root.remove(TestScoreBoardEventProvider.NUMBERED, child2));
        assertEquals(3, root.numberOf(TestScoreBoardEventProvider.NUMBERED));
        assertEquals(1, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals(child2, event.getValue());
        assertTrue(event.isRemove());
        assertEquals(9, ((TestScoreBoardEventProviderImpl) root).itemsAdded);
        assertEquals(5, ((TestScoreBoardEventProviderImpl) root).itemsRemoved);
        assertEquals(1, child1.getNumber());
        assertEquals(1, subchild1.getNumber());
        assertEquals(1, subsubchild1.getNumber());
        assertEquals(2, child2.getNumber());
        assertEquals(2, subchild2.getNumber());
        assertEquals(2, subsubchild2.getNumber());
        assertEquals(3, child4.getNumber());
        assertEquals(3, subchild4.getNumber());
        assertEquals(3, subsubchild4.getNumber());
        assertEquals(4, child5.getNumber());
        assertEquals(4, subchild5.getNumber());
        assertEquals(4, subsubchild5.getNumber());
        assertEquals(child1, root.get(TestScoreBoardEventProvider.NUMBERED, 1));
        assertNull(root.get(TestScoreBoardEventProvider.NUMBERED, 2));
        assertEquals(child4, root.get(TestScoreBoardEventProvider.NUMBERED, 3));
        assertEquals(child5, root.get(TestScoreBoardEventProvider.NUMBERED, 4));
        assertEquals(child2, child1.getNext());
        assertNull(child1.getPrevious());
        assertEquals(subchild2, subchild1.getNext());
        assertNull(subchild1.getPrevious());
        assertEquals(subsubchild2, subsubchild1.getNext());
        assertNull(subsubchild1.getPrevious());
        assertEquals(child4, child2.getNext());
        assertEquals(child1, child2.getPrevious());
        assertEquals(subchild4, subchild2.getNext());
        assertEquals(subchild1, subchild2.getPrevious());
        assertEquals(subsubchild4, subsubchild2.getNext());
        assertEquals(subsubchild1, subsubchild2.getPrevious());
        assertEquals(child5, child4.getNext());
        assertEquals(child2, child4.getPrevious());
        assertEquals(subchild5, subchild4.getNext());
        assertEquals(subchild2, subchild4.getPrevious());
        assertEquals(subsubchild5, subsubchild4.getNext());
        assertEquals(subsubchild2, subsubchild4.getPrevious());
        assertNull(child5.getNext());
        assertEquals(child4, child5.getPrevious());
        assertNull(subchild5.getNext());
        assertEquals(subchild4, subchild5.getPrevious());
        assertNull(subsubchild5.getNext());
        assertEquals(subsubchild4, subsubchild5.getPrevious());
    }

    @Test
    public void testExecute() {
        assertEquals(0, ((TestScoreBoardEventProviderImpl) root).commmandsExecuted);
        root.execute(TestScoreBoardEventProvider.TEST_COMMAND);
        assertEquals(1, ((TestScoreBoardEventProviderImpl) root).commmandsExecuted);
    }

    @Test
    public void testInverseReference() {
        TestScoreBoardEventProvider child1 = new TestScoreBoardEventProviderImpl(root, "ID1",
                TestScoreBoardEventProvider.MULTIPLE);
        TestScoreBoardEventProvider child2 = new TestScoreBoardEventProviderImpl(root, "ID2",
                TestScoreBoardEventProvider.MULTIPLE);
        assertNull(child1.get(TestScoreBoardEventProvider.REFERENCE));
        assertNull(child2.get(TestScoreBoardEventProvider.REFERENCE));
        assertEquals(0, root.numberOf(TestScoreBoardEventProvider.MULTIPLE));

        root.add(TestScoreBoardEventProvider.MULTIPLE, child1);
        assertEquals(root, child1.get(TestScoreBoardEventProvider.REFERENCE));

        child2.set(TestScoreBoardEventProvider.REFERENCE, root);
        assertEquals(child2, root.get(TestScoreBoardEventProvider.MULTIPLE, "ID2"));

        root.remove(TestScoreBoardEventProvider.MULTIPLE, "ID2");
        assertNull(child2.get(TestScoreBoardEventProvider.REFERENCE));
        assertEquals(root, child1.get(TestScoreBoardEventProvider.REFERENCE));
        assertEquals(1, root.numberOf(TestScoreBoardEventProvider.MULTIPLE));

        child1.set(TestScoreBoardEventProvider.REFERENCE, null);
        assertEquals(0, root.numberOf(TestScoreBoardEventProvider.MULTIPLE));
    }

    @Test
    public void testDirectCopy() {
        TestNumberedScoreBoardEventProvider child = new TestNumberedScoreBoardEventProviderImpl(root, 1);
        root.add(TestScoreBoardEventProvider.NUMBERED, child);

        assertEquals(0, (int) child.get(TestNumberedScoreBoardEventProvider.RO_DIRECT_COPY));
        assertEquals(0, (int) child.get(TestNumberedScoreBoardEventProvider.RW_DIRECT_COPY));

        child.set(TestNumberedScoreBoardEventProvider.RW_DIRECT_COPY, 2);
        assertEquals(2, (int) child.get(TestNumberedScoreBoardEventProvider.RO_DIRECT_COPY));
        assertEquals(2, (int) child.get(TestNumberedScoreBoardEventProvider.RW_DIRECT_COPY));

        child.set(TestNumberedScoreBoardEventProvider.RO_DIRECT_COPY, 3);
        assertEquals(2, (int) child.get(TestNumberedScoreBoardEventProvider.RO_DIRECT_COPY));
        assertEquals(2, (int) child.get(TestNumberedScoreBoardEventProvider.RW_DIRECT_COPY));

        root.set(TestScoreBoardEventProvider.INT, 4);
        assertEquals(4, (int) child.get(TestNumberedScoreBoardEventProvider.RO_DIRECT_COPY));
        assertEquals(4, (int) child.get(TestNumberedScoreBoardEventProvider.RW_DIRECT_COPY));
    }

    @Test
    public void testIndirectCopy() {
        TestScoreBoardEventProvider child = new TestScoreBoardEventProviderImpl(root, "ID1",
                TestScoreBoardEventProvider.MULTIPLE);
        root.add(TestScoreBoardEventProvider.MULTIPLE, child);

        assertEquals(0, (int) child.get(TestScoreBoardEventProvider.RO_INDIRECT_COPY));
        assertEquals(0, (int) child.get(TestScoreBoardEventProvider.RW_INDIRECT_COPY));

        child.set(TestScoreBoardEventProvider.RW_INDIRECT_COPY, 2);
        assertEquals(2, (int) child.get(TestScoreBoardEventProvider.RO_INDIRECT_COPY));
        assertEquals(2, (int) child.get(TestScoreBoardEventProvider.RW_INDIRECT_COPY));

        child.set(TestScoreBoardEventProvider.RO_INDIRECT_COPY, 3);
        assertEquals(2, (int) child.get(TestScoreBoardEventProvider.RO_INDIRECT_COPY));
        assertEquals(2, (int) child.get(TestScoreBoardEventProvider.RW_INDIRECT_COPY));

        root.set(TestScoreBoardEventProvider.INT, 4);
        assertEquals(4, (int) child.get(TestScoreBoardEventProvider.RO_INDIRECT_COPY));
        assertEquals(4, (int) child.get(TestScoreBoardEventProvider.RW_INDIRECT_COPY));
    }

    @Test
    public void testRecalcuate() {
        TestScoreBoardEventProvider child = new TestScoreBoardEventProviderImpl(root, "ID",
                TestScoreBoardEventProvider.MULTIPLE);
        root.add(TestScoreBoardEventProvider.MULTIPLE, child);

        assertEquals(0, (int) child.get(TestScoreBoardEventProvider.RECALCULATED));

        child.set(TestScoreBoardEventProvider.RECALCULATED, 5);
        assertEquals(-5, (int) child.get(TestScoreBoardEventProvider.RECALCULATED));

        child.set(TestScoreBoardEventProvider.INT, 3);
        assertEquals(5, (int) child.get(TestScoreBoardEventProvider.RECALCULATED));

        root.set(TestScoreBoardEventProvider.INT, 2);
        assertEquals(-5, (int) child.get(TestScoreBoardEventProvider.RECALCULATED));

        root.remove(TestScoreBoardEventProvider.MULTIPLE, child);
        assertEquals(5, (int) child.get(TestScoreBoardEventProvider.RECALCULATED));

        child.set(TestScoreBoardEventProvider.REFERENCE, root);
        assertEquals(-5, (int) child.get(TestScoreBoardEventProvider.RECALCULATED));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsOnSettingPropertyFromDifferentClass() {
        root.set(Jam.DURATION, 0L);
    }
}
