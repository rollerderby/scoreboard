package com.carolinarollergirls.scoreboard.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MirrorScoreBoardEventProviderImpl<M extends ScoreBoardEventProvider, C
                                                   extends MirrorScoreBoardEventProvider<M>>
    extends ScoreBoardEventProviderImpl<C> implements MirrorScoreBoardEventProvider<M> {
    @SuppressWarnings("unchecked")
    public MirrorScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, M sourceElement) {
        this(parent, sourceElement,
             (Child<C>) ((MirrorScoreBoardEventProviderImpl<?, ?>) parent)
                 .reversePropertyMap.get(((ScoreBoardEventProviderImpl<M>) sourceElement).ownType));
    }
    @SuppressWarnings("unchecked")
    public MirrorScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, M sourceElement, Child<C> type) {
        super(parent, sourceElement.getId(), type);
        this.sourceElement = sourceElement;
        fillMaps();
        Collection<Property<?>> props = new ArrayList<>(sourceElement.getProperties());
        props.removeAll(getProperties());
        for (Property<?> prop : props) {
            if (reversePropertyMap.containsKey(prop)) {
                addMirrorCopy((Child<? extends ScoreBoardEventProvider>) prop);
            } else {
                addProperties(prop);
                if (prop instanceof Value<?>) { setCopy((Value<?>) prop); }
                if (prop instanceof Child<?>) { setCopy((Child<?>) prop); }
            }
        }
    }
    public MirrorScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, String id, Child<C> type) {
        super(parent, id, type);
    }

    protected void fillMaps() {}

    @Override
    public String getProviderId() {
        return sourceElement.getProviderId();
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (source.isFile()) { return last; }
        return value;
    }

    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        synchronized (coreLock) {
            if (!source.isFile()) {
                sourceElement.getOrCreate(propertyMap.getOrDefault(prop, prop), id, source);
                return get(prop, id);
            }
            return null;
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        sourceElement.execute(prop, source);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ScoreBoardEventProvider> MirrorScoreBoardEventProvider<T> getMirror(Child<T> prop, String id) {
        return super.get((Child<MirrorScoreBoardEventProvider<T>>) reversePropertyMap.get(prop), id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ScoreBoardEventProvider> Collection<MirrorScoreBoardEventProvider<T>>
    getAllMirrors(Child<T> prop) {
        return super.getAll((Child<MirrorScoreBoardEventProvider<T>>) reversePropertyMap.get(prop));
    }

    @Override
    public M getSourceElement() {
        return sourceElement;
    }

    @SuppressWarnings("unchecked")
    protected <T extends ScoreBoardEventProvider> MirrorScoreBoardEventProvider<T> toMirror(T source) {
        if (source == null) { return null; }
        MirrorScoreBoardEventProvider<T> mirror =
            (MirrorScoreBoardEventProvider<T>) getElement(classMap.get(source.getProviderClass()), source.getId());
        if (mirror == null) {
            MirrorScoreBoardEventProviderImpl<?, ?> paren =
                (MirrorScoreBoardEventProviderImpl<?, ?>) toMirror(source.getParent());
            mirror = mirrorFactory.createMirror(paren, source);
        } else if (mirror.getParent() instanceof MirrorScoreBoardEventProvider &&
                   mirror.getParent() != toMirror(source.getParent())) {
            ((MirrorScoreBoardEventProviderImpl<?, ?>) mirror).parent = toMirror(source.getParent());
        }
        return mirror;
    }
    protected <T extends ScoreBoardEventProvider> T fromMirror(MirrorScoreBoardEventProvider<T> image) {
        if (image == null) { return null; }
        return image.getSourceElement();
    }

    protected <T> void setCopy(Value<T> prop) { setCopy(prop, sourceElement, prop, false); }
    protected <T extends ValueWithId> void setCopy(Child<T> prop) { setCopy(prop, sourceElement, prop, false); }

    protected <T extends ScoreBoardEventProvider> void addMirrorCopy(final Child<T> sourceProperty) {
        @SuppressWarnings("unchecked")
        Child<MirrorScoreBoardEventProvider<T>> targetProperty =
            (Child<MirrorScoreBoardEventProvider<T>>) reversePropertyMap.get(sourceProperty);
        addProperties(targetProperty);
        ScoreBoardListener l = new ConditionalScoreBoardListener<>(
            sourceElement, sourceProperty, new ChildToMirrorScoreBoardListener<>(this, targetProperty));
        sourceElement.addScoreBoardListener(l);
        providers.put(l, sourceElement);
        reverseCopyListeners.put(targetProperty,
                                 new ChildFromMirrorScoreBoardListener<>(sourceElement, sourceProperty));
        for (T element : sourceElement.getAll(sourceProperty)) { add(targetProperty, toMirror(element), Source.COPY); }
    }

    public class ChildToMirrorScoreBoardListener<T extends ScoreBoardEventProvider> implements ScoreBoardListener {
        public ChildToMirrorScoreBoardListener(ScoreBoardEventProvider targetElement,
                                               Child<MirrorScoreBoardEventProvider<T>> targetProperty) {
            this.targetElement = targetElement;
            this.targetProperty = targetProperty;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void scoreBoardChange(ScoreBoardEvent<?> event) {
            T value = (T) event.getValue();
            if (event.isRemove()) {
                targetElement.remove(targetProperty, value.getProviderId(), Source.COPY);
            } else {
                targetElement.add(targetProperty, toMirror(value), Source.COPY);
            }
        }

        protected ScoreBoardEventProvider targetElement;
        protected Child<MirrorScoreBoardEventProvider<T>> targetProperty;
    }

    public class ChildFromMirrorScoreBoardListener<T extends ScoreBoardEventProvider>
        extends CopyScoreBoardListener<MirrorScoreBoardEventProvider<T>> {
        public ChildFromMirrorScoreBoardListener(ScoreBoardEventProvider targetElement, Child<T> targetProperty) {
            super(null, null);
            this.targetElement = targetElement;
            this.targetProperty = targetProperty;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void scoreBoardChange(ScoreBoardEvent<?> event) {
            scoreBoardChange((ScoreBoardEvent<MirrorScoreBoardEventProvider<T>>) event, Source.COPY);
        }

        @Override
        public void scoreBoardChange(ScoreBoardEvent<MirrorScoreBoardEventProvider<T>> event, Source source) {
            if (targetElement == null) { return; }
            MirrorScoreBoardEventProvider<T> value = event.getValue();
            if (event.isRemove()) {
                targetElement.remove(targetProperty, value.getProviderId(), source);
            } else {
                targetElement.add(targetProperty, fromMirror(value), source);
            }
        }

        protected ScoreBoardEventProvider targetElement;
        protected Child<T> targetProperty;
    }

    @SafeVarargs
    public final void addPropertyMapping(Child<? extends ScoreBoardEventProvider>... sources) {
        for (Child<? extends ScoreBoardEventProvider> source : sources) {
            Child<? extends MirrorScoreBoardEventProvider<?>> target =
                new Child<>(classMap.get(source.getType()), source.getJsonName(), null);
            propertyMap.put(target, source);
            reversePropertyMap.put(source, target);
        }
    }

    protected M sourceElement;
    protected Map<Child<? extends MirrorScoreBoardEventProvider<?>>, Child<? extends ScoreBoardEventProvider>>
        propertyMap = new HashMap<>();
    protected Map<Child<? extends ScoreBoardEventProvider>, Child<? extends MirrorScoreBoardEventProvider<?>>>
        reversePropertyMap = new HashMap<>();

    public static void addClassMapping(Class<? extends ScoreBoardEventProvider> source,
                                       Class<? extends MirrorScoreBoardEventProvider<?>> mirror) {
        classMap.put(source, mirror);
        if (elements.get(mirror) == null) { elements.put(mirror, new HashMap<>()); }
    }

    private static Map<Class<? extends ScoreBoardEventProvider>, Class<? extends MirrorScoreBoardEventProvider<?>>>
        classMap = new HashMap<>();
    protected static MirrorFactory mirrorFactory;
}
