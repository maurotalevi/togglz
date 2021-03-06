package org.togglz.core.repository.cache;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;

/**
 * 
 * Unit test for {@link CachingStateRepository}.
 * 
 * @author Christian Kaltepoth
 * 
 */
public class CachingStateRepositoryTest {

    private StateRepository delegate;

    @Before
    public void setup() {
        delegate = Mockito.mock(StateRepository.class);
        Mockito.when(delegate.getFeatureState(DummyFeature.TEST))
                .thenReturn(new FeatureState(DummyFeature.TEST, true));
    }

    public void tearDown() {
        delegate = null;
    }

    @Test
    public void testCachingOfReadOperationsWithTimeToLife() throws InterruptedException {

        StateRepository repository = new CachingStateRepository(delegate, 10000);

        // do some lookups
        for (int i = 0; i < 10; i++) {
            assertTrue(repository.getFeatureState(DummyFeature.TEST).isEnabled());
            Thread.sleep(10);
        }

        // delegate only called once
        Mockito.verify(delegate).getFeatureState(DummyFeature.TEST);
        Mockito.verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testCachingOfReadOperationsWithoutTimeToLife() throws InterruptedException {

        StateRepository repository = new CachingStateRepository(delegate, 0);

        // do some lookups
        for (int i = 0; i < 10; i++) {
            assertTrue(repository.getFeatureState(DummyFeature.TEST).isEnabled());
            Thread.sleep(10);
        }

        // delegate only called once
        Mockito.verify(delegate).getFeatureState(DummyFeature.TEST);
        Mockito.verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testCacheExpiryBecauseOfTimeToLife() throws InterruptedException {

        long ttl = 5;
        StateRepository repository = new CachingStateRepository(delegate, ttl);

        // do some lookups
        for (int i = 0; i < 5; i++) {
            assertTrue(repository.getFeatureState(DummyFeature.TEST).isEnabled());
            Thread.sleep(ttl + 1); // wait some minimal amount of time to let the cache expire
        }

        // delegate called 5 times
        Mockito.verify(delegate, Mockito.times(5)).getFeatureState(DummyFeature.TEST);
        Mockito.verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testStateModifyExpiresCache() throws InterruptedException {

        StateRepository repository = new CachingStateRepository(delegate, 10000);

        // do some lookups
        for (int i = 0; i < 5; i++) {
            assertTrue(repository.getFeatureState(DummyFeature.TEST).isEnabled());
            Thread.sleep(10);
        }

        // now modify the feature state
        repository.setFeatureState(new FeatureState(DummyFeature.TEST, true));

        // do some lookups
        for (int i = 0; i < 5; i++) {
            assertTrue(repository.getFeatureState(DummyFeature.TEST).isEnabled());
            Thread.sleep(10);
        }

        // Check for the correct number of invocations
        Mockito.verify(delegate, Mockito.times(2)).getFeatureState(DummyFeature.TEST);
        Mockito.verify(delegate).setFeatureState(Mockito.any(FeatureState.class));
        Mockito.verifyNoMoreInteractions(delegate);

    }

    private enum DummyFeature implements Feature {
        TEST;
    }

}
