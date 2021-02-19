package com.mercadopago.android.px.internal.di;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import com.mercadopago.android.px.addons.BehaviourProvider;
import com.mercadopago.android.px.addons.ESCManagerBehaviour;
import com.mercadopago.android.px.addons.model.SecurityValidationData;
import com.mercadopago.android.px.configuration.PaymentConfiguration;
import com.mercadopago.android.px.core.MercadoPagoCheckout;
import com.mercadopago.android.px.core.internal.TrackingRepositoryModelMapper;
import com.mercadopago.android.px.internal.core.ApplicationModule;
import com.mercadopago.android.px.internal.datasource.AmountConfigurationRepositoryImpl;
import com.mercadopago.android.px.internal.datasource.AmountService;
import com.mercadopago.android.px.internal.datasource.CardTokenService;
import com.mercadopago.android.px.internal.datasource.ConfigurationSolver;
import com.mercadopago.android.px.internal.datasource.ConfigurationSolverImpl;
import com.mercadopago.android.px.internal.datasource.CongratsRepositoryImpl;
import com.mercadopago.android.px.internal.datasource.DiscountServiceImpl;
import com.mercadopago.android.px.internal.datasource.EscPaymentManagerImp;
import com.mercadopago.android.px.internal.datasource.ExperimentsRepositoryImpl;
import com.mercadopago.android.px.internal.datasource.ExpressMetadataRepositoryImpl;
import com.mercadopago.android.px.internal.datasource.CheckoutRepositoryImpl;
import com.mercadopago.android.px.internal.datasource.InstructionsService;
import com.mercadopago.android.px.internal.datasource.ModalRepositoryImpl;
import com.mercadopago.android.px.internal.datasource.PayerPaymentMethodRepositoryImpl;
import com.mercadopago.android.px.internal.datasource.PaymentMethodRepositoryImpl;
import com.mercadopago.android.px.internal.datasource.PaymentService;
import com.mercadopago.android.px.internal.datasource.PrefetchInitService;
import com.mercadopago.android.px.internal.datasource.TokenizeService;
import com.mercadopago.android.px.internal.features.PaymentResultViewModelFactory;
import com.mercadopago.android.px.internal.features.payment_congrats.model.PXPaymentCongratsTracking;
import com.mercadopago.android.px.internal.features.payment_congrats.model.PaymentCongratsModel;
import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository;
import com.mercadopago.android.px.internal.repository.AmountRepository;
import com.mercadopago.android.px.internal.repository.CardTokenRepository;
import com.mercadopago.android.px.internal.repository.CongratsRepository;
import com.mercadopago.android.px.internal.repository.DiscountRepository;
import com.mercadopago.android.px.internal.repository.EscPaymentManager;
import com.mercadopago.android.px.internal.repository.ExperimentsRepository;
import com.mercadopago.android.px.internal.repository.ExpressMetadataRepository;
import com.mercadopago.android.px.internal.repository.CheckoutRepository;
import com.mercadopago.android.px.internal.repository.InstructionsRepository;
import com.mercadopago.android.px.internal.repository.ModalRepository;
import com.mercadopago.android.px.internal.repository.PayerPaymentMethodRepository;
import com.mercadopago.android.px.internal.repository.PaymentMethodRepository;
import com.mercadopago.android.px.internal.repository.PaymentRepository;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.repository.TokenRepository;
import com.mercadopago.android.px.internal.services.CheckoutService;
import com.mercadopago.android.px.internal.services.CongratsService;
import com.mercadopago.android.px.internal.services.GatewayService;
import com.mercadopago.android.px.internal.services.InstructionsClient;
import com.mercadopago.android.px.internal.tracking.TrackingRepository;
import com.mercadopago.android.px.internal.util.TextUtil;
import com.mercadopago.android.px.model.Device;
import com.mercadopago.android.px.services.MercadoPagoServices;
import com.mercadopago.android.px.tracking.internal.MPTracker;

import static com.mercadopago.android.px.internal.util.MercadoPagoUtil.getPlatform;

public final class Session extends ApplicationModule {

    /**
     * This singleton instance is safe because session will work with application applicationContext. Application
     * applicationContext it's never leaking.
     */
    @SuppressLint("StaticFieldLeak")
    private static Session instance;

    // mem cache - lazy init.
    private final CheckoutConfigurationModule configurationModule;
    private DiscountRepository discountRepository;
    private AmountRepository amountRepository;
    private CheckoutRepository checkoutRepository;
    private PaymentRepository paymentRepository;
    private AmountConfigurationRepository amountConfigurationRepository;
    private InstructionsService instructionsRepository;
    private CardTokenRepository cardTokenRepository;
    private CongratsRepository congratsRepository;
    private ExperimentsRepository experimentsRepository;
    private EscPaymentManagerImp escPaymentManager;
    private MPTracker tracker;
    private PaymentResultViewModelFactory paymentResultViewModelFactory;
    private ViewModelModule viewModelModule;
    private PayerPaymentMethodRepository payerPaymentMethodRepository;
    private ExpressMetadataRepository expressMetadataRepository;
    private PaymentMethodRepository paymentMethodRepository;
    private ModalRepository modalRepository;
    private ConfigurationSolver configurationSolver;
    private final NetworkModule networkModule;

    private Session(@NonNull final Context context) {
        super(context);
        configurationModule = new CheckoutConfigurationModule(context);
        networkModule = new NetworkModule(context);
    }

    @NonNull
    public static Session getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "Session is not initialized. Make sure to call Session.initialize(Context) first.");
        }
        return instance;
    }

    @NonNull
    public static Session initialize(@NonNull final Context context) {
        instance = new Session(context);
        ConfigurationModule.initialize(instance.configurationModule);
        return instance;
    }

    /**
     * Initialize Session with MercadoPagoCheckout information.
     *
     * @param mercadoPagoCheckout non mutable checkout intent.
     */
    public void init(@NonNull final MercadoPagoCheckout mercadoPagoCheckout) {
        clear();
        configIds(mercadoPagoCheckout);

        // Store persistent paymentSetting
        final CheckoutConfigurationModule configurationModule = getConfigurationModule();

        final PaymentConfiguration paymentConfiguration = mercadoPagoCheckout.getPaymentConfiguration();
        final PaymentSettingRepository paymentSetting = configurationModule.getPaymentSettings();
        paymentSetting.configure(mercadoPagoCheckout.getPublicKey());
        paymentSetting.configure(mercadoPagoCheckout.getAdvancedConfiguration());
        paymentSetting.configurePrivateKey(mercadoPagoCheckout.getPrivateKey());
        paymentSetting.configure(paymentConfiguration);
        resolvePreference(mercadoPagoCheckout, paymentSetting);
        // end Store persistent paymentSetting
    }

    public void init(@NonNull final PaymentCongratsModel paymentCongratsModel) {
        clear();
        final PXPaymentCongratsTracking trackingData = paymentCongratsModel.getPxPaymentCongratsTracking();
        configurationModule.getTrackingRepository().configure(
            new TrackingRepository.Model(trackingData.getSessionId(), trackingData.getFlow(),
                trackingData.getFlowExtraInfo()));
    }

    @NonNull
    public State getSessionState() {
        try {
            if (configurationModule.getPaymentSettings().getPaymentConfiguration() != null) {
                return Session.State.VALID;
            } else {
                return Session.State.UNKNOWN;
            }
        } catch (final Exception e) {
            return Session.State.INVALID;
        }
    }

    private void resolvePreference(@NonNull final MercadoPagoCheckout mercadoPagoCheckout,
        final PaymentSettingRepository paymentSetting) {
        final String preferenceId = mercadoPagoCheckout.getPreferenceId();

        if (TextUtil.isEmpty(preferenceId)) {
            paymentSetting.configure(mercadoPagoCheckout.getCheckoutPreference());
        } else {
            //Pref cerrada.
            paymentSetting.configurePreferenceId(preferenceId);
        }
    }

    private void clear() {
        getPaymentRepository().reset();
        getExperimentsRepository().reset();
        getConfigurationModule().reset();
        getExperimentsRepository().reset();
        getPayerPaymentMethodRepository().reset();
        getPaymentMethodRepository().reset();
        getModalRepository().reset();
        getAmountConfigurationRepository().reset();
        getDiscountRepository().reset();
        networkModule.reset();
        discountRepository = null;
        amountRepository = null;
        checkoutRepository = null;
        paymentRepository = null;
        instructionsRepository = null;
        amountConfigurationRepository = null;
        cardTokenRepository = null;
        congratsRepository = null;
        escPaymentManager = null;
        viewModelModule = null;
        expressMetadataRepository = null;
        payerPaymentMethodRepository = null;
        paymentMethodRepository = null;
        modalRepository = null;
        configurationSolver = null;
    }

    @NonNull
    public CheckoutRepository getCheckoutRepository() {
        if (checkoutRepository == null) {
            final PaymentSettingRepository paymentSettings = getConfigurationModule().getPaymentSettings();
            checkoutRepository = new CheckoutRepositoryImpl(paymentSettings, getExperimentsRepository(),
                configurationModule.getDisabledPaymentMethodRepository(), getMercadoPagoESC(),
                networkModule.getRetrofitClient().create(CheckoutService.class),
                configurationModule.getTrackingRepository(), getTracker(),
                getPayerPaymentMethodRepository(), getExpressMetadataRepository(), getPaymentMethodRepository(),
                getModalRepository(), getConfigurationModule().getPayerComplianceRepository(),
                getAmountConfigurationRepository(), getDiscountRepository()) {
            };
        }
        return checkoutRepository;
    }

    @NonNull
    public ExperimentsRepository getExperimentsRepository() {
        if (experimentsRepository == null) {
            experimentsRepository = new ExperimentsRepositoryImpl(getSharedPreferences());
        }

        return experimentsRepository;
    }

    @NonNull
    public ESCManagerBehaviour getMercadoPagoESC() {
        final TrackingRepository trackingRepository = configurationModule.getTrackingRepository();
        return BehaviourProvider
            .getEscManagerBehaviour(trackingRepository.getSessionId(), trackingRepository.getFlowId());
    }

    @NonNull
    private Device getDevice() {
        return new Device(getApplicationContext(), getMercadoPagoESC());
    }

    @NonNull
    public MercadoPagoServices getMercadoPagoServices() {
        final PaymentSettingRepository paymentSettings = getConfigurationModule().getPaymentSettings();
        return new MercadoPagoServices(getApplicationContext(), paymentSettings.getPublicKey(),
            paymentSettings.getPrivateKey());
    }

    @NonNull
    public AmountRepository getAmountRepository() {
        if (amountRepository == null) {
            amountRepository = new AmountService(configurationModule.getPaymentSettings(),
                configurationModule.getChargeRepository(), getDiscountRepository());
        }
        return amountRepository;
    }

    @NonNull
    public DiscountRepository getDiscountRepository() {
        if (discountRepository == null) {
            discountRepository =
                new DiscountServiceImpl(getFileManager(), getConfigurationModule().getUserSelectionRepository(),
                    getAmountConfigurationRepository(), getConfigurationSolver());
        }
        return discountRepository;
    }

    @NonNull
    public AmountConfigurationRepository getAmountConfigurationRepository() {
        if (amountConfigurationRepository == null) {
            amountConfigurationRepository = new AmountConfigurationRepositoryImpl(getFileManager(),
                getConfigurationModule().getUserSelectionRepository(), getConfigurationSolver());
        }
        return amountConfigurationRepository;
    }

    @NonNull
    public CheckoutConfigurationModule getConfigurationModule() {
        return configurationModule;
    }

    @NonNull
    public PaymentRepository getPaymentRepository() {
        if (paymentRepository == null) {
            paymentRepository = new PaymentService(configurationModule.getUserSelectionRepository(),
                configurationModule.getPaymentSettings(),
                configurationModule.getDisabledPaymentMethodRepository(),
                getDiscountRepository(),
                getAmountRepository(),
                getApplicationContext(),
                getEscPaymentManager(),
                getMercadoPagoESC(),
                getTokenRepository(),
                getInstructionsRepository(),
                getAmountConfigurationRepository(),
                getCongratsRepository(),
                getFileManager(),
                MapperProvider.INSTANCE.getFromPayerPaymentMethodIdToCardMapper(),
                MapperProvider.INSTANCE.getPaymentMethodMapper(),
                getPaymentMethodRepository());
        }

        return paymentRepository;
    }

    @NonNull
    public EscPaymentManager getEscPaymentManager() {
        if (escPaymentManager == null) {
            escPaymentManager = new EscPaymentManagerImp(getMercadoPagoESC(), configurationModule.getPaymentSettings());
        }
        return escPaymentManager;
    }

    @NonNull
    private TokenRepository getTokenRepository() {
        return new TokenizeService(networkModule.getRetrofitClient().create(GatewayService.class),
            getConfigurationModule().getPaymentSettings(), getMercadoPagoESC(), getDevice(), getTracker());
    }

    @NonNull
    public InstructionsRepository getInstructionsRepository() {
        if (instructionsRepository == null) {
            instructionsRepository =
                new InstructionsService(getConfigurationModule().getPaymentSettings(),
                    networkModule.getRetrofitClient().create(InstructionsClient.class));
        }
        return instructionsRepository;
    }

    @NonNull
    public CardTokenRepository getCardTokenRepository() {
        if (cardTokenRepository == null) {
            final GatewayService gatewayService =
                networkModule.getRetrofitClient().create(GatewayService.class);
            cardTokenRepository = new CardTokenService(gatewayService, getConfigurationModule().getPaymentSettings(),
                getDevice(), getMercadoPagoESC());
        }
        return cardTokenRepository;
    }

    @NonNull
    public CongratsRepository getCongratsRepository() {
        if (congratsRepository == null) {
            final CongratsService congratsService = networkModule.getRetrofitClient().create(CongratsService.class);
            final PaymentSettingRepository paymentSettings = getConfigurationModule().getPaymentSettings();
            congratsRepository = new CongratsRepositoryImpl(congratsService, paymentSettings,
                getPlatform(getApplicationContext()), configurationModule.getTrackingRepository(),
                configurationModule.getUserSelectionRepository(), getAmountRepository(),
                configurationModule.getDisabledPaymentMethodRepository(),
                configurationModule.getPayerComplianceRepository(), getMercadoPagoESC(), getExpressMetadataRepository(),
                MapperProvider.INSTANCE.getAlternativePayerPaymentMethodsMapper());
        }
        return congratsRepository;
    }

    @NonNull
    public ViewModelModule getViewModelModule() {
        if (viewModelModule == null) {
            viewModelModule = new ViewModelModule();
        }
        return viewModelModule;
    }

    public PayerPaymentMethodRepository getPayerPaymentMethodRepository() {
        if (payerPaymentMethodRepository == null) {
            payerPaymentMethodRepository = new PayerPaymentMethodRepositoryImpl(getFileManager());
        }
        return payerPaymentMethodRepository;
    }

    public ExpressMetadataRepository getExpressMetadataRepository() {
        if (expressMetadataRepository == null) {
            expressMetadataRepository = new ExpressMetadataRepositoryImpl(getFileManager(),
                getConfigurationModule().getDisabledPaymentMethodRepository());
        }
        return expressMetadataRepository;
    }

    public PaymentMethodRepository getPaymentMethodRepository() {
        if (paymentMethodRepository == null) {
            paymentMethodRepository = new PaymentMethodRepositoryImpl(getFileManager());
        }
        return paymentMethodRepository;
    }

    public ModalRepository getModalRepository() {
        if (modalRepository == null) {
            modalRepository = new ModalRepositoryImpl(getFileManager());
        }
        return modalRepository;
    }

    public ConfigurationSolver getConfigurationSolver() {
        if (configurationSolver == null) {
            configurationSolver = new ConfigurationSolverImpl(getPayerPaymentMethodRepository());
        }
        return configurationSolver;
    }

    @NonNull
    public PrefetchInitService getPrefetchInitService(@NonNull final MercadoPagoCheckout checkout) {
        configIds(checkout);
        return new PrefetchInitService(checkout, networkModule.getRetrofitClient().create(
            CheckoutService.class),
            getMercadoPagoESC(), configurationModule.getTrackingRepository());
    }

    @NonNull
    public MPTracker getTracker() {
        if (tracker == null) {
            tracker = new MPTracker(configurationModule.getTrackingRepository());
        }
        return tracker;
    }

    @NonNull
    public PaymentResultViewModelFactory getPaymentResultViewModelFactory() {
        if (paymentResultViewModelFactory == null) {
            paymentResultViewModelFactory = new PaymentResultViewModelFactory(getTracker());
        }
        return paymentResultViewModelFactory;
    }

    private void configIds(@NonNull final MercadoPagoCheckout checkout) {
        //Favoring product id in discount params because that one is surely custom if exists
        final String deprecatedProductId =
            checkout.getAdvancedConfiguration().getDiscountParamsConfiguration().getProductId();
        final String productId = TextUtil.isNotEmpty(deprecatedProductId) ? deprecatedProductId
            : checkout.getAdvancedConfiguration().getProductId();
        configurationModule.getTrackingRepository().configure(
            TrackingRepositoryModelMapper.INSTANCE.map(checkout.getTrackingConfiguration()));
        final boolean securityEnabled = BehaviourProvider.getSecurityBehaviour()
            .isSecurityEnabled(new SecurityValidationData.Builder(productId).build());
        getTracker().setSecurityEnabled(securityEnabled);
        configurationModule.getProductIdProvider().configure(productId);
    }

    public enum State {
        VALID,
        INVALID,
        UNKNOWN
    }
}
