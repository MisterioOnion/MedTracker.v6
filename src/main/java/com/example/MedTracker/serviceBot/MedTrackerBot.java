package com.example.MedTracker.serviceBot;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.example.MedTracker.configuration.MedTrackerBotConfiguration;
import com.example.MedTracker.model.*;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@PropertySource("application.properties")
public class MedTrackerBot extends TelegramLongPollingBot {


    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MedicationRepository medicationRepository;
    @Autowired
    private ReminderTimeRepository reminderTimeRepository;
    @Autowired
    private UserService userService;

    final MedTrackerBotConfiguration config;
    private Map<Long, Integer> requestCounts = new HashMap<>();
    private Map<Long, Long> firstRequestTimes = new HashMap<>();

    // Определение состояний ввода данных
    enum InputState {
        NAME,
        DESCRIPTION,
        START_DATE,
        DURATION,
        DAYS_BETWEEN_DOSES,
        HH_MM,
    }

    // Переменная для хранения текущего состояния ввода данных
    InputState currentState;

    // Переменные для хранения промежуточных значений
    String medicationName;
    String description;
    String startDate;
    String todayDate;
    int duration;
    boolean isRepeated;
    int daysBetweenDoses;
    int frequency;
    int counter = 0;
    String ADD_MEDICATION_TEXT = "Пожалуйста введите информацию о вашем лекарстве.\n\n" +
            "Последовательно в следующем формате:\n" +
            "- название лекарства,\n" +
            "- описание лекарства,\n" +
            "- дату начала в формате (гггг-мм-дд),\n" +
            "- количество дней приёма лекарства,\n" +
            "- если курс препарата будет проводиться повторно нажмите Да " +
            "и затем укажите количество дней через " +
            "которое следует начать повторный приём," +
            " в противном случае нажмите Нет\n" +
            "- также укажите какое количество раз в день вы будете принимать препарат, " +
            "нажав на соответствующую кнопку от 1 до 3." +
            "Не забудьте ввести для них время)\n\n" +
            "Пример ввода:\n" +
            "Препарат A, краткое описание, 2023-07-08, 7, Да, 10, 3\n" +
            "Препарат A, краткое описание, 2023-07-08, 7, Нет, 3";

    static final String HELP_TEXT =
            "Вы можете выполнять команды из главного меню слева или введя команду:\n\n" +
                    "Введите /start, чтобы увидеть приветственное сообщение\n\n" +
                    "Введите /history_medication, чтобы просмотреть сохраненные данные о ваших лекарствах\n\n" +
                    "Введите /add_medication, чтобы добавить новое лекарство\n\n" +
                    "Введите /help, чтобы снова увидеть это сообщение";
    static final String START_TEXT ="Привет! \n" +
            "Я — бот, который напоминает о приеме лекарств.\n" +
            "Люди нередко забывают вовремя выпить таблетку или вкусную витаминку и расстраиваются из-за этого.\n" +
            "В некоторых случаях своевременный прием лекарств критически необходим для правильного лечения.\n" +
            "\n" +
            "Я внимательно слежу за тем, чтобы вы ничего не забыли и присылаю напоминания.\n";
    String HH_MM_TEXT="Введите будильники в формате ЧЧ:ММ, где ЧЧ это часы, а ММ это минуты";
    static final String ERROR_TEXT = "ERROR occurred: ";
    static final String YES_REGISTER_BUTTON = "YES_REGISTER_BUTTON";
    static final String NO_REGISTER_BUTTON = "NO_REGISTER_BUTTON";
    static final String YES_IS_REPEATED_BUTTON = "YES_IS_REPEATED_BUTTON";
    static final String NO_IS_REPEATED_BUTTON = "NO_IS_REPEATED_BUTTON";
    static final String ONE_FREQUENCY_BUTTON = "ONE_FREQUENCY_BUTTON";
    static final String TWO_FREQUENCY_BUTTON = "TWO_FREQUENCY_BUTTON";
    static final String THREE_FREQUENCY_BUTTON = "THREE_FREQUENCY_BUTTON";

    private static Connection connection;
    private boolean addFlag = false;

    public MedTrackerBot( MedTrackerBotConfiguration config) {

        this.config = config;

        try {
            connection = DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        List<BotCommand> listOfCommands = List.of(
                new BotCommand("/start", "get a welcome message"),
                new BotCommand("/help", "info how use this bot"),
                new BotCommand("/add_medication", "add new medication"),
                new BotCommand("/history_medication", "history medication"),
                new BotCommand("/set_reminder", "set a reminder"),
                new BotCommand("/delete_reminder", "delete reminder")

        );
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(),null));
        } catch (TelegramApiException e) {
            log.error("ERROR setting bot's command list" + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    /*обработка пользовательских команд*/
    @Override
    public void onUpdateReceived(Update update){

        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            limitMessage(chatId, messageText);

            if(messageText.contains("/send") && config.getAdminId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user: users){
                    prepareAndSendMarkdownMessage(user.getChatId(), textToSend, false);
                }
            } else {
                // Проверка, является ли пользователь зарегистрированным
                User user = userRepository.findById(chatId).orElse(null);
                boolean isRegistration = user != null && user.isRegistration();

                if (isRegistration) {
                    switch (messageText) {
                        case "/start":
                            String text = EmojiParser.parseToUnicode(START_TEXT + "С чего начнём?" + ":relaxed:");
                            sendMessage(chatId, text);
                            break;

                        case "/help":
                            prepareAndSendMarkdownMessage(chatId, HELP_TEXT, false);
                            break;

                        case "Начать приём нового лекарства":
                        case "/add_medication":
                            this.addFlag = true;
                            prepareAndSendMarkdownMessage(chatId, ADD_MEDICATION_TEXT, false);
                            InputMedicationMessage(messageText, chatId);
                            break;

                        case "/register":
                            String text_ = EmojiParser.parseToUnicode("Вы уже зарегистрированы." + ":relaxed:");
                            sendMessage(chatId, text_);
                            break;

                        case "Посмотр истории приёма":
                        case "/history_medication":
                            sendMedicationsToTelegram(chatId);
                            break;

                        default:
                            if(!this.addFlag) {
                                String texts = EmojiParser.parseToUnicode(
                                        "Извините, команда пока не работает" + ":disappointed:"
                                );
                                prepareAndSendMarkdownMessage(chatId, texts, false);
                            }
                            else {
                                InputMedicationMessage(messageText, chatId);
                            }
                    }
                } else {
                    switch (messageText) {

                        case "/register":
                            register(chatId);
                            break;

                        default:
                            String text = EmojiParser.parseToUnicode(
                                    "Извините, но вы пока не зарегистрированы" + ":disappointed:"+
                                            "\nПройдите регистрацию /register");
                            prepareAndSendMarkdownMessage(chatId, text, false);
                    }
                }
            }
        }else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if(callbackData.equals(YES_REGISTER_BUTTON)){
                Message message = update.getCallbackQuery().getMessage();
                if (message != null) {
                    registerUser(message);
                    String text = EmojiParser.parseToUnicode(
                            message.getChat().getFirstName() +
                                    ", добро пожаловать в MedTrackerBot!" +
                                    ":partying_face:"
                    );
                    executeEditMessageText(text, chatId, messageId);

                    log.info("Replied to USER " + message.getChat().getFirstName());
                } else {
                    log.error("Received null message in onUpdateReceived");
                }
            }
            if(callbackData.equals(NO_REGISTER_BUTTON)){
                String text = EmojiParser.parseToUnicode(
                        "Жаль.." + ":disappointed:" + " Но мы будем ждать вас." + ":hugs:"
                );
                executeEditMessageText(text, chatId, messageId);
            }

            if (callbackData.equals(YES_IS_REPEATED_BUTTON)) {
                isRepeated = true;
                executeEditMessageText("Повторный приём предусмотрен.", chatId, messageId);
                currentState = InputState.DAYS_BETWEEN_DOSES;
                prepareAndSendMarkdownMessage(chatId,"Введите через сколько дней после окончания приема начнется новый прием.", false);

            }
            if (callbackData.equals(NO_IS_REPEATED_BUTTON)) {
                isRepeated = false;
                executeEditMessageText("Повторный приём не предусмотрен.", chatId, messageId);
                currentState = InputState.HH_MM;
                frequency(chatId);

            }

            if(callbackData.equals(ONE_FREQUENCY_BUTTON)){
                frequency = 1;
                addMedicationToBD(chatId);

                String text = EmojiParser.parseToUnicode("Напоминание будет приходить 1 раз в день");
                executeEditMessageText(text, chatId, messageId);
                currentState = InputState.HH_MM;
                sendMessage(chatId, HH_MM_TEXT);

            }
            if(callbackData.equals(TWO_FREQUENCY_BUTTON)){
                frequency = 2;
                addMedicationToBD(chatId);

                String text = EmojiParser.parseToUnicode("Напоминание будет приходить 2 раза в день");
                executeEditMessageText(text, chatId, messageId);
                currentState = InputState.HH_MM;
                sendMessage(chatId, HH_MM_TEXT);
            }
            if(callbackData.equals(THREE_FREQUENCY_BUTTON)){
                frequency = 3;
                addMedicationToBD(chatId);

                String text = EmojiParser.parseToUnicode("Напоминание будет приходить 3 раза в день");
                executeEditMessageText(text, chatId, messageId);
                currentState = InputState.HH_MM;
                sendMessage(chatId, HH_MM_TEXT);

            }
        }
    }

    /*получение данных о медикаментах по Id пользователя*/
    protected List<Medication> getMedications(long chatId) {
        //User user = userRepository.findById(chatId).orElse(null);
        //return medicationRepository.findByUser(user);

        Optional<User> userOptional = userRepository.findFirstByChatId(chatId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return medicationRepository.findByUser(user);
        } else {
            return Collections.emptyList();
        }
    }

    /*  отправка сообщений в телеграмм*/
    private void sendMedicationsToTelegram(long chatId) {
        List<Medication> medications = getMedications(chatId);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("*Ваши лекарства:*\n\n");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        for (Medication medication : medications) {
            // Форматируйте данные лекарств по вашему усмотрению
            String medicationInfo = String.format(
                    "*Название:* %s\n*Описание:* %s\n*Начало:* %s\n*Длительность:* " +
                            "%d дней\n*Частота приема в день:* %d\n",
                    medication.getMedicineName(),
                    medication.getDescription(),
                    medication.getStartDate().format(dateFormatter),
                    medication.getDaysForEndDate(),
                    medication.getDosesForDay());

            if (medication.getIsRepeated()) {
                medicationInfo += String.format(
                        "*Повторный приём:* Да\n*Перерыв между приемами:* %d дней\n\n",
                        medication.getDaysBetweenDoses());

            } else {
                medicationInfo += String.format("*Повторный приём:* Нет\n\n");
            }

            messageBuilder.append(medicationInfo);
        }
        String messageText = messageBuilder.toString();
        prepareAndSendMarkdownMessage(chatId, messageText, true);
    }

    /* Метод для обработки входящего сообщения про лекарства */
    private void InputMedicationMessage(String userInput, long chatId) {
        if (currentState == null) {
            currentState = InputState.NAME;
            prepareAndSendMarkdownMessage(chatId, "Введите название лекарства.", false);
        } else {
            switch (currentState) {
                case NAME:
                    medicationName = userInput.trim();
                    currentState = InputState.DESCRIPTION;
                    prepareAndSendMarkdownMessage(chatId, "Введите описание лекарства.", false);
                    break;
                case DESCRIPTION:
                    description = userInput.trim();
                    currentState = InputState.START_DATE;
                    prepareAndSendMarkdownMessage(
                            chatId, "Введите дату начала приема (в формате ГГГГ-ММ-ДД).", false);
                    break;
                case START_DATE:
                    startDate = userInput.trim();
                    // Проверка корректности формата даты
                    if (!isValidDateFormat(startDate)) {
                        prepareAndSendMarkdownMessage(chatId,
                                "Некорректный формат даты. Пожалуйста, введите дату в формате ГГГГ-ММ-ДД, " +
                                "где ГГГГ это год, ММ - месяц, ДД - день в числовом формате!", false);
                        break;
                    }
                    // Проверка, чтобы дата начала приема не была меньше текущей даты
                    if (isStartDateBeforeToday(startDate)) {
                        prepareAndSendMarkdownMessage(
                                chatId,
                                "Дата начала приема не может быть ранее текущей даты!",
                                false);
                        break;
                    }
                    currentState = InputState.DURATION;
                    prepareAndSendMarkdownMessage(
                            chatId, "Введите длительность приема в днях.", false);

                    break;
                case DURATION:
                    try {
                        duration = Integer.parseInt(userInput.trim());
                        //currentState = InputState.IS_REPEATED;
                        isRepeated(chatId);
                    } catch (NumberFormatException e) {
                        prepareAndSendMarkdownMessage(
                                chatId,
                                "Некорректный ввод. Пожалуйста, введите целое число для длительности приема!",
                                false);
                    }

                    break;
                case HH_MM:
                    try {
                        if((counter < frequency) && (isValidTimeFormat(userInput.trim()))){
                            InputRemiderTimeMessage(userInput.trim(), chatId, frequency);
                            counter++;
                            String text = String.format("Вы внесли %d из %d будильников", counter, frequency);
                            sendMessage(chatId, text);
                            if (counter == frequency) {
                                sendMessage(chatId, "Все будильники были введены, больше ввести нельзя!! ");
                                this.addFlag = false;
                                currentState = null;
                                counter = 0;
                            }
                        } else {
                            sendMessage(chatId, "Наверный формат ввода времени." +
                            "Введите время в формате HH:mm, где HH - часы не более 23, а mm - минуты не более 59");
                        }

                    } catch (Exception e) {
                        prepareAndSendMarkdownMessage(chatId, String.valueOf(e), false);
                        currentState = null;
                    }
                    break;

                case DAYS_BETWEEN_DOSES:
                    try {
                        daysBetweenDoses = Integer.parseInt(userInput.trim());
                        frequency(chatId);


                    } catch (NumberFormatException e) {
                        prepareAndSendMarkdownMessage(
                                chatId,
                                "Некорректный ввод. Пожалуйста, введите целое число для перерыва между пр" +
                                        "иемами!", false);
                    }
                    break;
                default:
                    prepareAndSendMarkdownMessage(
                            chatId,
                            "Ошибка ввода данных. Некорректное состояние, почему ты тут?",
                            false);
                    currentState = null;
                    break;
            }
        }
    }



    private void frequency(long chatId){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Cколько раз в день будет приниматься лекарство?");

        String[] buttonTexts = {"1 раз", "2 раза", "3 раза"};
        String[] callbackDatas = {ONE_FREQUENCY_BUTTON, TWO_FREQUENCY_BUTTON, THREE_FREQUENCY_BUTTON};
        InlineKeyboardMarkup markupInLine = createInlineKeyboardMarkup(buttonTexts, callbackDatas);

        message.setReplyMarkup(markupInLine);

        executeMessage(message);

    }

    private void isRepeated(long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Будет ли повторный приём?");

        String[] buttonTexts = {"Да", "Нет"};
        String[] callbackDatas = {YES_IS_REPEATED_BUTTON, NO_IS_REPEATED_BUTTON};
        InlineKeyboardMarkup markupInLine = createInlineKeyboardMarkup(buttonTexts, callbackDatas);

        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }


    /* Метод для проверки корректности формата даты */
    private boolean isValidDateFormat(String date) {
        String regex = "\\d{4}-\\d{2}-\\d{2}"; // Формат ГГГГ-ММ-ДД
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(date);

        if (!matcher.matches()) {return false;}

        try {
            LocalDate parsedDate = LocalDate.parse(date);
            int month = parsedDate.getMonthValue();
            int day = parsedDate.getDayOfMonth();

            if (month > 12 || month < 1) {return false;}

            if (day > 31 || day < 1) {return false;}
        } catch (DateTimeParseException e) {return false;}

        return true;
    }

    /* Метод для проверки корректности формата времени */
    private boolean isValidTimeFormat(String timeStr) {
        String pattern = "^(?:[01]\\d|2[0-3]):[0-5]\\d$";
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(timeStr);

        if (!matcher.matches()) {return false;}

        return true;
    }

    /* Метод для проверки, что дата начала приема не ранее текущей даты */
    private boolean isStartDateBeforeToday(String startDate) {
        LocalDate currentDate = LocalDate.now();
        LocalDate inputDate = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
        return inputDate.isBefore(currentDate);
    }

    /* метод проверяет сколько запросов было введено в минуту,
    и если превышает 20 то выводится сообшение о превышение*/
    private void limitMessage(long chatId,String messageText){

        int requestCount = requestCounts.getOrDefault(chatId, 0);

        if (requestCount == 0) {
            firstRequestTimes.put(chatId, System.currentTimeMillis());
        }
        if (requestCount >= 20 && System.currentTimeMillis() - firstRequestTimes.get(chatId) < 60000){
            String errorMessage = "Превышен лимит сообщений в минуту!\nПовторить свой запрос позднее";
            sendMessage(chatId, errorMessage);
        }
        if (requestCount >= 20 && System.currentTimeMillis() - firstRequestTimes.get(chatId) >= 60000) {
            requestCounts.put(chatId, requestCount = 0);
            firstRequestTimes.put(chatId, System.currentTimeMillis());
        }
        if (messageText != null && messageText.startsWith("/")) {
            requestCounts.put(chatId, requestCount + 1);
        }
    }

    /*проверка ошибок для callback кнопок + добавляется текст*/
    private void executeEditMessageText(String text, long chatId, long messageId){
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    /*проверка не произошла ли ошибка*/
    private void executeMessage(SendMessage message){
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    /*кнопки (согласен/несогласен) стать новымпользователем*/
    private void register(long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Вы точно хотите зарегестрироваться?");

        String[] buttonTexts = {"Да", "Нет"};
        String[] callbackDatas = {YES_REGISTER_BUTTON, NO_REGISTER_BUTTON};
        InlineKeyboardMarkup markupInLine = createInlineKeyboardMarkup(buttonTexts, callbackDatas);

        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    /*создание начальных кнопок под сообщением в чате горизонтальных*/
    private InlineKeyboardMarkup createInlineKeyboardMarkup(String[] buttonTexts, String[] callbackDatas) {
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        for (int i = 0; i < buttonTexts.length; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonTexts[i]);
            button.setCallbackData(callbackDatas[i]);

            rowInLine.add(button);
        }

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        return markupInLine;
    }

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void checkMedicationReminders() {
        Iterable<User> usersIterable = userRepository.findAll();
        List<User> users = StreamSupport
                .stream(usersIterable.spliterator(), false)
                .collect(Collectors.toList());
        for (User user : users) {
            long chatId = user.getChatId();
            checkReminderTimesForUser(chatId);
        }
    }

    public void checkReminderTimesForUser(long chatId) {
        User user = userRepository.findById(chatId).orElse(null);

        if (user != null) {
            List<ReminderTime> reminderTimes = reminderTimeRepository.findByMedication_User(user);
            LocalTime currentTime = LocalTime.now();

            for (ReminderTime reminderTime : reminderTimes) {
                LocalTime medicationTime = reminderTime.getTime();

                if (medicationTime.equals(currentTime)) {
                    Medication medication = reminderTime.getMedication();
                    String message = "Пора принять лекарство: "
                            + medication.getMedicineName()
                            + "\nОписание: "
                            + medication.getDescription();
                    sendMessage(chatId, message);
                }
            }
        }
    }

    private void InputRemiderTimeMessage(String userInput, long chatId, int frequency){
        addReminderTimeToBD(chatId, medicationName, userInput);
    }
    private void addReminderTimeToBD(long chatId, String medicationName, String remiderTime){
        addReminderTime(medicationName, remiderTime, chatId);
        sendMessage(chatId, "Будильник успешно добавлен в базу данных.");
    }

    /* Создание SQL-запроса для добавления новой записи в базу данных */
   protected void addReminderTime(String medicationName, String time, long chatId) {
       try {
           User user = userRepository.findById(chatId).orElse(null);
           Medication medication = medicationRepository.findByMedicineNameAndUser(medicationName, user);

           if (medication != null) {

               SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
               Date parsedTime = timeFormat.parse(time);
               LocalTime localTime = parsedTime.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();

               ReminderTime reminderTime = new ReminderTime();
               reminderTime.setMedication(medication);
               reminderTime.setTime(localTime);
               reminderTimeRepository.save(reminderTime);
           } else {
               sendMessage(chatId, "Такого будьника нет!! Что-то ты сделал не так дорогой друг");
           }
       } catch (Exception e) {
           log.error(ERROR_TEXT + e.getMessage());
       }
   }


    private void addMedicationToBD(long chatId){
        addMedication(chatId, medicationName, description, startDate, todayDate, duration, isRepeated, daysBetweenDoses, frequency);
        // Отправка подтверждения пользователю
        sendMessage(chatId, "Данные о лекарстве успешно добавлены в базу данных.");
        currentState = InputState.HH_MM;
    }

    /* Создание SQL-запроса для добавления новой записи в базу данных */
    protected void addMedication(long chatId, String name, String description, String startDate, String todayDate, int duration, boolean isRepeated, int daysBetweenDoses, int frequency) {
        try {
            Medication medication = new Medication();
            User user = userRepository.findById(chatId).orElse(null);

            medication.setUser(user);
            medication.setId(chatId);
            medication.setMedicineName(name);
            medication.setDescription(description);

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate parsedStartDate = LocalDate.parse(startDate, dateFormatter);
            medication.setStartDate(parsedStartDate);
            medication.setTodayDate(new Timestamp(System.currentTimeMillis()));
            medication.setDaysForEndDate(duration);
            medication.setIsRepeated(isRepeated);
            medication.setDaysBetweenDoses(daysBetweenDoses);
            medication.setDosesForDay(frequency);
            medicationRepository.save(medication);
        } catch (Exception e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    /*регистрация нового пользователя*/
    private void registerUser(Message msg) {

        if (msg != null) {
            if (userRepository.findById(msg.getChatId()).isEmpty()) {

                var chatId = msg.getChatId();
                var chat = msg.getChat();

                User user = new User();

                user.setChatId(chatId);
                user.setFirstName(chat.getFirstName());
                user.setLastName(chat.getLastName());
                user.setUserName(chat.getUserName());
                user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
                user.setRegistration(true);

                userRepository.save(user);
                log.info("\nUSER saved: " + user);
            }
        }else {
            log.error("Received null message in registerUser");
        }
    }

    /*создание кнопок внизу*/
    private ReplyKeyboardMarkup createKeyboardMarkup (String[]...buttonNames){
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        for (String[] rowNames : buttonNames) {
            KeyboardRow row = new KeyboardRow();
            for (String buttonName : rowNames) {
                row.add(buttonName);
            }
            keyboardRows.add(row);
        }

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    /*отпрака сообщения с возможностью сделать текст жирным*/
    private void prepareAndSendMarkdownMessage(long chatId, String textToSend, boolean parseMarkdown) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        message.enableMarkdown(parseMarkdown);

        executeMessage(message);
    }


    /*отпрака сообщения + отображение начальных кнопок*/
    private void sendMessage ( long chatId, String textToSend){

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = createKeyboardMarkup(
                new String[]{"Просмотр истории приёма"},
                new String[]{"Текущий приём"},
                new String[]{"Начать приём нового лекарства"}
        );
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }


}


