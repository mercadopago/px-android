package com.mercadopago.android.px.addons;

import androidx.annotation.NonNull;

public final class PXBehaviourConfigurer {

    private SecurityBehaviour securityBehaviour;
    private ESCManagerBehaviour escManagerBehaviour;
    private TrackingBehaviour trackingBehaviour;
    private FlowBehaviour flowBehaviour;

    public PXBehaviourConfigurer with(@NonNull final ESCManagerBehaviour escManagerBehaviour) {
        this.escManagerBehaviour = escManagerBehaviour;
        return this;
    }

    public PXBehaviourConfigurer with(@NonNull final SecurityBehaviour securityBehaviour) {
        this.securityBehaviour = securityBehaviour;
        return this;
    }

    public PXBehaviourConfigurer with(@NonNull final TrackingBehaviour trackingBehaviour) {
        this.trackingBehaviour = trackingBehaviour;
        return this;
    }

    @Deprecated
    public PXBehaviourConfigurer with(@NonNull final LocaleBehaviour localeBehaviour) {
        return this;
    }

    public PXBehaviourConfigurer with(@NonNull final FlowBehaviour flowBehaviour) {
        this.flowBehaviour = flowBehaviour;
        return this;
    }

    public void configure() {
        BehaviourProvider.set(securityBehaviour);
        BehaviourProvider.set(escManagerBehaviour);
        BehaviourProvider.set(trackingBehaviour);
        BehaviourProvider.set(flowBehaviour);
    }
}