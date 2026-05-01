package ru.otus.trackingbot.bot;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.*;
import ru.otus.trackingbot.bot.keyboard.KeyboardFactory;
import ru.otus.trackingbot.config.BotInfoConfig;
import ru.otus.trackingbot.constant.BotConstants;
import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.User;
import ru.otus.trackingbot.entity.UserParcel;
import ru.otus.trackingbot.service.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrackingBot тесты")
class TrackingBotTest {

    @Mock
    private UserService userService;

    @Mock
    private ParcelService parcelService;

    @Mock
    private UserParcelService userParcelService;

    @Mock
    private TrackingCacheService trackingCacheService;

    @Mock
    private TrackingServiceFactory trackingServiceFactory;

    @Mock
    private KeyboardFactory keyboardFactory;

    @Mock
    private BotInfoConfig botInfoConfig;

    @Mock
    private AbstractTrackingService trackingService;

    private TrackingBot trackingBot;

    private static final Long CHAT_ID = 123456789L;
    private static final Integer MESSAGE_ID = 1;
    private static final String BOT_TOKEN = "test-token";
    private static final String BOT_USERNAME = "testBot";

    private User testUser;

    @BeforeEach
    void setUp() {
        // Создаем бота с отключенной регистрацией команд
        TrackingBot originalBot = new TrackingBot(
                BOT_TOKEN,
                BOT_USERNAME,
                true,
                userService,
                parcelService,
                userParcelService,
                trackingCacheService,
                trackingServiceFactory,
                keyboardFactory,
                botInfoConfig);

        trackingBot = spy(originalBot);

        testUser = User.builder()
                .id(1L)
                .chatId(CHAT_ID)
                .firstName("TestUser")
                .notificationEnabled(true)
                .build();
    }

    @Test
    @DisplayName("/start - вызывает getOrCreateUser и getMainKeyboard")
    void onStartCommand_callsGetOrCreateUserAndGetMainKeyboard() {
        Update update = createTextUpdate(BotConstants.CMD_START);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(userService, times(2)).getOrCreateUser(eq(CHAT_ID), any(), any(), any());
        verify(keyboardFactory).getMainKeyboard();
    }

    @Test
    @DisplayName("/menu - вызывает getMainKeyboard")
    void onMenuCommand_callsGetMainKeyboard() {
        Update update = createTextUpdate(BotConstants.CMD_MENU);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(keyboardFactory).getMainKeyboard();
    }

    @Test
    @DisplayName("/track - вызывает getCancelKeyboard")
    void onTrackCommand_callsGetCancelKeyboard() {
        Update update = createTextUpdate(BotConstants.CMD_TRACK);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(keyboardFactory.getCancelKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(keyboardFactory).getCancelKeyboard();
    }

    @Test
    @DisplayName("/list - когда есть посылки, вызывает getAllUserParcelsWithDetails и getParcelsKeyboard")
    void onListCommand_WhenParcelsExist_callsGetAllUserParcelsWithDetailsAndGetParcelsKeyboard() {
        Update update = createTextUpdate(BotConstants.CMD_LIST);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);

        List<UserParcel> userParcels = createUserParcelsList(2);
        when(userParcelService.getAllUserParcelsWithDetails(testUser)).thenReturn(userParcels);
        when(keyboardFactory.getParcelsKeyboard(anyList())).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(userParcelService).getAllUserParcelsWithDetails(testUser);
        verify(keyboardFactory).getParcelsKeyboard(anyList());
        verify(keyboardFactory, never()).getMainKeyboard();
    }

    @Test
    @DisplayName("/list - когда нет посылок, вызывает getAllUserParcelsWithDetails и getMainKeyboard")
    void onListCommand_WhenNoParcels_callsGetAllUserParcelsWithDetailsAndGetMainKeyboard() {
        Update update = createTextUpdate(BotConstants.CMD_LIST);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(userParcelService.getAllUserParcelsWithDetails(testUser)).thenReturn(Collections.emptyList());
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(userParcelService).getAllUserParcelsWithDetails(testUser);
        verify(keyboardFactory).getMainKeyboard();
        verify(keyboardFactory, never()).getParcelsKeyboard(anyList());
    }

    @Test
    @DisplayName("/help - вызывает getSupport")
    void onHelpCommand_callsGetSupport() {
        Update update = createTextUpdate(BotConstants.CMD_HELP);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(botInfoConfig.getSupport()).thenReturn("@support");
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(botInfoConfig).getSupport();
        verify(keyboardFactory).getMainKeyboard();
    }

    @Test
    @DisplayName("Неизвестная команда - вызывает getMainKeyboard")
    void onUnknownCommand_callsGetMainKeyboard() {
        Update update = createTextUpdate("/unknown");
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(keyboardFactory).getMainKeyboard();
    }

    @Test
    @DisplayName(
            "Кнопка 'Мои посылки' - когда есть посылки, вызывает getAllUserParcelsWithDetails и getParcelsKeyboard")
    void onMyParcelsButton_WhenParcelsExist_callsGetAllUserParcelsWithDetailsAndGetParcelsKeyboard() {
        Update update = createTextUpdate(BotConstants.BTN_MY_PARCELS);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);

        List<UserParcel> userParcels = createUserParcelsList(2);
        when(userParcelService.getAllUserParcelsWithDetails(testUser)).thenReturn(userParcels);
        when(keyboardFactory.getParcelsKeyboard(anyList())).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(userParcelService).getAllUserParcelsWithDetails(testUser);
        verify(keyboardFactory).getParcelsKeyboard(anyList());
        verify(keyboardFactory, never()).getMainKeyboard();
    }

    @Test
    @DisplayName("Кнопка 'Мои посылки' - когда нет посылок, вызывает getAllUserParcelsWithDetails и getMainKeyboard")
    void onMyParcelsButton_WhenNoParcels_callsGetAllUserParcelsWithDetailsAndGetMainKeyboard() {
        Update update = createTextUpdate(BotConstants.BTN_MY_PARCELS);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(userParcelService.getAllUserParcelsWithDetails(testUser)).thenReturn(Collections.emptyList());
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(userParcelService).getAllUserParcelsWithDetails(testUser);
        verify(keyboardFactory).getMainKeyboard();
        verify(keyboardFactory, never()).getParcelsKeyboard(anyList());
    }

    @Test
    @DisplayName("Кнопка 'Отследить посылку' - вызывает getCancelKeyboard")
    void onTrackParcelButton_callsGetCancelKeyboard() {
        Update update = createTextUpdate(BotConstants.BTN_TRACK_PARCEL);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(keyboardFactory.getCancelKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(keyboardFactory).getCancelKeyboard();
    }

    @Test
    @DisplayName("Кнопка 'Статистика' - вызывает getActiveUserParcels")
    void onStatisticsButton_callsGetActiveUserParcels() {
        Update update = createTextUpdate(BotConstants.BTN_STATISTICS);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(userParcelService.getActiveUserParcels(testUser)).thenReturn(Collections.emptyList());
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(userParcelService).getActiveUserParcels(testUser);
        verify(keyboardFactory).getMainKeyboard();
    }

    @Test
    @DisplayName("Кнопка 'Уведомления' - вызывает getNotificationsKeyboard")
    void onNotificationsButton_callsGetNotificationsKeyboard() {
        Update update = createTextUpdate(BotConstants.BTN_NOTIFICATIONS);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(keyboardFactory.getNotificationsKeyboard(true)).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(keyboardFactory).getNotificationsKeyboard(true);
    }

    @Test
    @DisplayName("Кнопка 'Помощь' - вызывает getSupport")
    void onHelpButton_callsGetSupport() {
        Update update = createTextUpdate(BotConstants.BTN_HELP);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(botInfoConfig.getSupport()).thenReturn("@support");
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(botInfoConfig).getSupport();
        verify(keyboardFactory).getMainKeyboard();
    }

    @Test
    @DisplayName("Кнопка 'О боте' - вызывает методы BotInfoConfig")
    void onAboutButton_callsBotInfoConfigMethods() {
        Update update = createTextUpdate(BotConstants.BTN_ABOUT);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(botInfoConfig.getVersion()).thenReturn("1.0.0");
        when(botInfoConfig.getDeveloper()).thenReturn("Developer");
        when(botInfoConfig.getCopyright()).thenReturn("Copyright");
        when(botInfoConfig.getSupport()).thenReturn("@support");
        when(botInfoConfig.getTechnologies()).thenReturn(Collections.emptyList());
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(botInfoConfig).getVersion();
        verify(botInfoConfig).getDeveloper();
        verify(botInfoConfig).getCopyright();
        verify(botInfoConfig).getSupport();
        verify(botInfoConfig, atLeastOnce()).getTechnologies();
        verify(keyboardFactory).getMainKeyboard();
    }

    @Test
    @DisplayName("Callback back_to_menu - вызывает getMainKeyboard")
    void onBackToMenuCallback_callsGetMainKeyboard() {
        Update update = createCallbackUpdate(BotConstants.CALLBACK_BACK_TO_MENU);
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(keyboardFactory).getMainKeyboard();
    }

    @Test
    @DisplayName("Callback notifications_on - вызывает toggleNotifications")
    void onNotificationsOnCallback_callsToggleNotifications() {
        Update update = createCallbackUpdate(BotConstants.CALLBACK_NOTIFICATIONS_ON);
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(userService).toggleNotifications(CHAT_ID, true);
        verify(keyboardFactory).getMainKeyboard();
    }

    @Test
    @DisplayName("Callback notifications_off - вызывает toggleNotifications")
    void onNotificationsOffCallback_callsToggleNotifications() {
        Update update = createCallbackUpdate(BotConstants.CALLBACK_NOTIFICATIONS_OFF);
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(userService).toggleNotifications(CHAT_ID, false);
        verify(keyboardFactory).getMainKeyboard();
    }

    @Test
    @DisplayName(
            "Callback back_to_parcels - когда есть посылки, вызывает getAllUserParcelsWithDetails и getParcelsKeyboard")
    void onBackToParcelsCallback_WhenParcelsExist_callsGetAllUserParcelsWithDetailsAndGetParcelsKeyboard() {
        Update update = createCallbackUpdate(BotConstants.CALLBACK_BACK_TO_PARCELS);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);

        List<UserParcel> userParcels = createUserParcelsList(2);
        when(userParcelService.getAllUserParcelsWithDetails(testUser)).thenReturn(userParcels);
        when(keyboardFactory.getParcelsKeyboard(anyList())).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(userParcelService).getAllUserParcelsWithDetails(testUser);
        verify(keyboardFactory).getParcelsKeyboard(anyList());
        verify(keyboardFactory, never()).getMainKeyboard();
    }

    @Test
    @DisplayName(
            "Callback back_to_parcels - когда нет посылок, вызывает getAllUserParcelsWithDetails и getMainKeyboard")
    void onBackToParcelsCallback_WhenNoParcels_callsGetAllUserParcelsWithDetailsAndGetMainKeyboard() {
        Update update = createCallbackUpdate(BotConstants.CALLBACK_BACK_TO_PARCELS);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(userParcelService.getAllUserParcelsWithDetails(testUser)).thenReturn(Collections.emptyList());
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(userParcelService).getAllUserParcelsWithDetails(testUser);
        verify(keyboardFactory).getMainKeyboard();
        verify(keyboardFactory, never()).getParcelsKeyboard(anyList());
    }

    @Test
    @DisplayName("Callback parcel_stop - вызывает stopParcelTracking и показывает обновленное меню")
    void onParcelStopCallback_callsStopParcelTrackingAndShowsMenu() {
        Long parcelId = 123L;
        Update update = createCallbackUpdate(BotConstants.PREFIX_PARCEL_STOP + "_" + parcelId);

        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);

        UserParcel userParcel = createUserParcel(parcelId, true);
        when(userParcelService.findByIdWithDetails(eq(parcelId), eq(testUser))).thenReturn(Optional.of(userParcel));
        when(userParcelService.stopTracking(eq(testUser), anyString())).thenReturn(true);
        when(keyboardFactory.getParcelActionsKeyboard(eq(parcelId), eq(false))).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(userParcelService).stopTracking(eq(testUser), anyString());
        verify(keyboardFactory).getParcelActionsKeyboard(eq(parcelId), eq(false));
    }

    @Test
    @DisplayName("Callback parcel_resume - вызывает resumeParcelTracking и показывает обновленное меню")
    void onParcelResumeCallback_callsResumeParcelTrackingAndShowsMenu() {
        Long parcelId = 123L;
        Update update = createCallbackUpdate(BotConstants.PREFIX_PARCEL_RESUME + "_" + parcelId);

        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);

        UserParcel userParcel = createUserParcel(parcelId, false);
        when(userParcelService.findByIdWithDetails(eq(parcelId), eq(testUser))).thenReturn(Optional.of(userParcel));
        when(keyboardFactory.getParcelActionsKeyboard(eq(parcelId), eq(true))).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(userParcelService).updateUserParcelStatus(any(UserParcel.class), isNull());
        verify(keyboardFactory).getParcelActionsKeyboard(eq(parcelId), eq(true));
    }

    @Test
    @DisplayName("Callback parcel_delete - вызывает deleteParcel и показывает подтверждение")
    void onParcelDeleteCallback_callsDeleteParcelAndShowsConfirmation() {
        Long parcelId = 123L;
        Update update = createCallbackUpdate(BotConstants.PREFIX_PARCEL_DELETE + "_" + parcelId);

        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);

        UserParcel userParcel = createUserParcel(parcelId, true);
        when(userParcelService.findByIdWithDetails(eq(parcelId), eq(testUser))).thenReturn(Optional.of(userParcel));
        when(keyboardFactory.getConfirmDeleteKeyboard(eq(parcelId))).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(keyboardFactory).getConfirmDeleteKeyboard(eq(parcelId));
    }

    @Test
    @DisplayName("Callback parcel_delete_confirm - вызывает confirmDeleteParcel")
    void onParcelDeleteConfirmCallback_callsConfirmDeleteParcel() {
        Long parcelId = 123L;
        Update update = createCallbackUpdate(BotConstants.PREFIX_PARCEL_DELETE_CONFIRM + "_" + parcelId);

        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);

        UserParcel userParcel = createUserParcel(parcelId, true);
        when(userParcelService.findByIdWithDetails(eq(parcelId), eq(testUser))).thenReturn(Optional.of(userParcel));
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(userParcelService).deleteUserParcel(any(UserParcel.class));
        verify(keyboardFactory).getMainKeyboard();
    }

    @Test
    @DisplayName("Callback с действием parcel_info_123 - вызывает findByIdWithDetails")
    void onParcelInfoCallback_callsFindByIdWithDetails() {
        Long parcelId = 123L;
        Update update = createCallbackUpdate(BotConstants.PREFIX_PARCEL_INFO + "_" + parcelId);
        when(userService.getOrCreateUser(eq(CHAT_ID), any(), any(), any())).thenReturn(testUser);
        when(userParcelService.findByIdWithDetails(eq(parcelId), eq(testUser))).thenReturn(Optional.empty());

        trackingBot.onUpdateReceived(update);

        verify(userParcelService).findByIdWithDetails(eq(parcelId), eq(testUser));
    }

    @Test
    @DisplayName("Callback с некорректным ID - вызывает getMainKeyboard (обработка ошибки)")
    void onCallbackWithInvalidId_callsGetMainKeyboard() {
        Update update = createCallbackUpdate("parcel_info_invalid");
        when(keyboardFactory.getMainKeyboard()).thenReturn(null);

        trackingBot.onUpdateReceived(update);

        verify(keyboardFactory).getMainKeyboard();
    }

    @Test
    @DisplayName("Обновление без сообщения - нет вызовов")
    void onUpdateWithoutMessage_hasNoInteractions() {
        Update update = new Update();

        trackingBot.onUpdateReceived(update);

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Обновление с пустым сообщением - нет вызовов")
    void onUpdateWithEmptyMessage_hasNoInteractions() {
        Update update = new Update();
        Message message = new Message();
        update.setMessage(message);

        trackingBot.onUpdateReceived(update);

        verifyNoInteractions(userService);
    }

    // =====================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =====================================================

    private Update createTextUpdate(String text) {
        Update update = new Update();
        Message mockMessage = mock(Message.class);
        when(mockMessage.getChatId()).thenReturn(CHAT_ID);
        when(mockMessage.getText()).thenReturn(text);
        when(mockMessage.hasText()).thenReturn(true);

        org.telegram.telegrambots.meta.api.objects.User telegramUser =
                new org.telegram.telegrambots.meta.api.objects.User();
        telegramUser.setId(123L);
        telegramUser.setFirstName("Test");
        telegramUser.setUserName("testuser");
        when(mockMessage.getFrom()).thenReturn(telegramUser);

        update.setMessage(mockMessage);
        return update;
    }

    private Update createCallbackUpdate(String callbackData) {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData(callbackData);

        Message mockMessage = mock(Message.class);
        when(mockMessage.getChatId()).thenReturn(CHAT_ID);
        when(mockMessage.getMessageId()).thenReturn(MESSAGE_ID);
        callbackQuery.setMessage(mockMessage);

        update.setCallbackQuery(callbackQuery);
        return update;
    }

    private UserParcel createUserParcel(Long id, boolean isActive) {
        Parcel parcel = Parcel.builder()
                .trackingNumber("TRACK" + id)
                .serviceName("Почта России")
                .build();

        return UserParcel.builder()
                .id(id)
                .user(testUser)
                .parcel(parcel)
                .isActive(isActive)
                .customName(null)
                .lastStatus("В пути")
                .build();
    }

    private List<UserParcel> createUserParcelsList(int count) {
        List<UserParcel> parcels = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Parcel parcel = Parcel.builder()
                    .trackingNumber("TRACK" + i)
                    .serviceName("Почта России")
                    .build();

            UserParcel userParcel = UserParcel.builder()
                    .id((long) i)
                    .user(testUser)
                    .parcel(parcel)
                    .isActive(true)
                    .customName(null)
                    .lastStatus("В пути")
                    .addedAt(LocalDateTime.now())
                    .build();
            parcels.add(userParcel);
        }
        return parcels;
    }
}
