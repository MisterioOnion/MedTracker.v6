package com.example.MedTracker.serviceBot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.example.MedTracker.configuration.MedTrackerBotConfiguration;
import com.example.MedTracker.model.Medication;
import com.example.MedTracker.model.MedicationRepository;
import com.example.MedTracker.model.User;
import com.example.MedTracker.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.sql.Timestamp;
/*
@Slf4j
@Component
public class MedTrackerBot extends TelegramLongPollingBot {


    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MedicationRepository medicationRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private MedicationService medicationService;
    boolean startWait = false; // <- Инициализация булеан переменной в самом начал

    final MedTrackerBotConfiguration config;
    private Map<Long, Integer> requestCounts = new HashMap<>();
    private Map<Long, Long> firstRequestTimes = new HashMap<>();
    private Map<Long, String> waitingInputs = new HashMap<>();

    String ADD_MEDICATION_TEXT = "Пожалуйста введите информацию о вашем лекарстве.\n\n" +
            "Через запятую введите:\n" +
            "- название,\n" +
            "- описание,\n" +
            "- дату начала в формате (гггг-мм-дд),\n" +
            "- количество дней приёма,\n" +
            "- если курс препарата будет проводиться повторно введите количество дней через " +
            "которое следует начать повторный приём,\n" +
            "- также укажите какое количество раз в день вы будете принимать препарат.\n\n" +
            "Пример ввода:\n" +
            "Препарат A, краткое описание, 2023-06-08, 7, 14, 3";

    static final String HELP_TEXT =
            "Вы можете выполнять команды из главного меню слева или введя команду:\n\n" +
                    "Введите /start, чтобы увидеть приветственное сообщение\n\n" +
                    "Введите /mydata, чтобы просмотреть сохраненные данные о себе\n\n" +
                    "Введите /help, чтобы снова увидеть это сообщение";
    static final String START_TEXT ="Привет! \n" +
            "Я — бот, который напоминает о приеме лекарств.\n" +
            "Люди нередко забывают вовремя выпить таблетку или вкусную витаминку и расстраиваются из-за этого.\n" +
            "В некоторых случаях своевременный прием лекарств критически необходим для правильного лечения.\n" +
            "\n" +
            "Я внимательно слежу за тем, чтобы вы ничего не забыли и присылаю напоминания.\n";
    String ERROR_INTEGER_MASSAGE = "Ошибка преобразования числового значения.\n" +
            "Пожалуйста, введите целочисленные значения для кол-ва дней, интервала и колл-ва доз в день.";
    String ERROR_TIMESTAMP_MASSAGE = "Ошибка преобразования даты и времени.\n" +
            "Пожалуйста, введите дату в формате: yyyy-MM-dd";
    String VALID_MESSAGE = "Ошибка: поля('Дата начала', " +
            "'Название лекарства', 'Количество дней приема', " +
            "'Количество таблеток в день') " +
            "должны быть заполнены корректно и не должны быть пустыми";

    String ERROR_INPUT = "Ошибка ввода данных. Пожалуйста, введите данные в формате:" +
            " название, описание, дата начала, количество дней, интервал, количество раз в день.";

    String INFO_TEXT = "Введите данные о лекарстве (название, описание, начало, конец, интервал): ";
    static final String ERROR_TEXT = "ERROR occurred: ";
    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";

    //@Autowired
    public MedTrackerBot( MedTrackerBotConfiguration config) {

        this.config = config;

        List<BotCommand> listOfCommands = List.of(
                new BotCommand("/start", "get a welcome message"),
                new BotCommand("/help", "info how use this bot"),
                new BotCommand("/mydata", "get your data stored"),
                new BotCommand("/deletdata", "delete my data"),
                new BotCommand("/settings", "set your preferences"),
                new BotCommand("/add_medication", "add new medication"),
                new BotCommand("/delete_medication", "add new medication")

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

    @Override
    public void onUpdateReceived(Update update){ //обработка пользовательских команд

        //var originalMessage = update.getMessage();
        //System.out.println(originalMessage.getText());

        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (waitingInputs.containsKey(chatId)) {
                // Если ожидается ввод от пользователя
                processUserInput(chatId, messageText);
            }
            // else {
            //    // Обработка команд и сообщений от пользователя
            //    if (messageText.equals("/add_medication")) {
            //        sendAddMedicationMessage(chatId);
            //    }
            //    // Остальная обработка команд и сообщений...
            //}

            limitMessage(chatId, messageText);

            if(messageText.contains("/send") && config.getAdminId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user: users){
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            } else {
                // Проверка, является ли пользователь зарегистрированным
                User user = userRepository.findById(chatId).orElse(null);
                boolean isRegistration = user != null && user.isRegistration();

                if (isRegistration) {
                    switch (messageText) {
                        case "/start":
                            String text = EmojiParser.parseToUnicode(
                                    START_TEXT +
                                            "С чего начнём?" + ":relaxed:"
                            );
                            sendMessage(chatId, text);
                            break;

                        case "/help":
                            prepareAndSendMessage(chatId, HELP_TEXT);
                            break;

                        case "/add_medication":
                            sendAddMedicationMessage(chatId);

                            addMedication(update);

                            break;

                        case "/register":
                            String text_ = EmojiParser.parseToUnicode(
                                    "Вы уже зарегистрированы." + ":relaxed:"
                            );
                            sendMessage(chatId, text_);
                            break;

                        default:
                            String texts = EmojiParser.parseToUnicode(
                                    "Извините, команда пока не работает" + ":disappointed:"
                            );
                            prepareAndSendMessage(chatId, texts);

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
                            prepareAndSendMessage(chatId, text);
                    }
                }
            }
        }else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if(callbackData.equals(YES_BUTTON)){
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
            else if(callbackData.equals(NO_BUTTON)){
                String text = EmojiParser.parseToUnicode(
                        "Жаль.." + ":disappointed:" + " Но мы будем ждать вас." + ":hugs:"
                );
                executeEditMessageText(text, chatId, messageId);
            }
        }
    }

    /* метод проверяет сколько запросов было введено в минуту,
    и если превышает 20 то выводится сообшение о превышение*/
//    private void limitMessage(long chatId,String messageText){
//
//        int requestCount = requestCounts.getOrDefault(chatId, 0);
//
//        if (requestCount == 0) {
//            firstRequestTimes.put(chatId, System.currentTimeMillis());
//        }
//        if (requestCount >= 20 && System.currentTimeMillis() - firstRequestTimes.get(chatId) < 60000){
//            String errorMessage = "Превышен лимит сообщений в минуту!\nПовторить свой запрос позднее";
//            sendMessage(chatId, errorMessage);
//        }
//        if (requestCount >= 20 && System.currentTimeMillis() - firstRequestTimes.get(chatId) >= 60000) {
//            requestCounts.put(chatId, requestCount = 0);
//            firstRequestTimes.put(chatId, System.currentTimeMillis());
//        }
//        if (messageText != null && messageText.startsWith("/")) {
//            requestCounts.put(chatId, requestCount + 1);
//        }
//    }
//
//    /*проверка ошибок для callback кнопок + текст*/
//    private void executeEditMessageText(String text, long chatId, long messageId){
//        EditMessageText message = new EditMessageText();
//        message.setChatId(String.valueOf(chatId));
//        message.setText(text);
//        message.setMessageId((int) messageId);
//
//        try {
//            execute(message);
//        } catch (TelegramApiException e) {
//            log.error(ERROR_TEXT + e.getMessage());
//        }
//    }
//
//    /*проверка не произошла ли ошибка*/
//    private void executeMessage(SendMessage message){
//        try {
//            execute(message);
//        } catch (TelegramApiException e) {
//            log.error(ERROR_TEXT + e.getMessage());
//        }
//    }
//
//    /*Начальный ввод для добавления лекрств в бд*/
//    private void sendAddMedicationMessage(long chatId) {
//
//        SendMessage message = new SendMessage();
//        message.setChatId(String.valueOf(chatId));
//        message.setText(ADD_MEDICATION_TEXT);
//
//        waitingInputs.put(chatId, "medicationData");
//
//        executeMessage(message);
//    }
//
//    /*пользовательский ввод*/
//    private void processUserInput(long chatId, String messageText) {
//        String waitingInput = waitingInputs.get(chatId);
//
//        if (waitingInput.equals("medicationData")) {
//            // Обработка введенных данных о лекарстве
//            processMedicationData(chatId, messageText);
//        }
//        // Другие варианты ожидаемого ввода...
//    }
//
//    /*Разбор и обработка введенных данных о лекарстве*/
//    private void processMedicationData(long chatId, String messageText) {
//        String[] inputData = messageText.split(",");
//        if (inputData.length == 6) {
//            // Данные введены корректно, сохранение лекарства в базе данных
//            prepareAndSendMessage(chatId, "Лекарство успешно добавлено!");
//        } else {
//            // Некорректный ввод данных, повторный запрос
//            prepareAndSendMessage(chatId, ERROR_INPUT);
//            sendAddMedicationMessage(chatId);
//        }
//    }
//
//    /*кнопки (согласен/несогласен) стать новымпользователем*/
//    private void register(long chatId) {
//
//        SendMessage message = new SendMessage();
//        message.setChatId(String.valueOf(chatId));
//        message.setText("Вы точно хотите зарегестрироваться?");
//        InlineKeyboardMarkup markupInLine = createInlineKeyboardMarkup(
//                "Да", YES_BUTTON,
//                "Нет", NO_BUTTON
//        );
//        message.setReplyMarkup(markupInLine);
//
//        executeMessage(message);
//    }
//
//    /*создание начальных кнопок под сообщением в чате*/
//    private InlineKeyboardMarkup createInlineKeyboardMarkup(String buttonText1, String callbackData1, String buttonText2, String callbackData2) {
//        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
//        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
//        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
//
//        InlineKeyboardButton button1 = new InlineKeyboardButton();
//        button1.setText(buttonText1);
//        button1.setCallbackData(callbackData1);
//
//        InlineKeyboardButton button2 = new InlineKeyboardButton();
//        button2.setText(buttonText2);
//        button2.setCallbackData(callbackData2);
//
//        rowInLine.add(button1);
//        rowInLine.add(button2);
//        rowsInLine.add(rowInLine);
//
//        markupInLine.setKeyboard(rowsInLine);
//        return markupInLine;
//    }
//
//    /*регистрация нового пользователя*/
//    private void registerUser(Message msg) {
//
//        if (msg != null) {
//            if (userRepository.findById(msg.getChatId()).isEmpty()) {
//
//                var chatId = msg.getChatId();
//                var chat = msg.getChat();
//
//                User user = new User();
//
//                user.setChatId(chatId);
//                user.setFirstName(chat.getFirstName());
//                user.setLastName(chat.getLastName());
//                user.setUserName(chat.getUserName());
//                user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
//                user.setRegistration(true);
//
//                userRepository.save(user);
//                log.info("\nUSER saved: " + user);
//            }
//        }else {
//            log.error("Received null message in registerUser");
//        }
//    }

  /*  private void addMedication(Message msg) {

        if (medicationRepository.findById(msg.getChatId()).isEmpty()) {

            var chatId = msg.getChatId();
            var chat = msg.getChat();

            String input = userInput.getMessage().getText();
            System.out.println("input");
            System.out.println(input);

            String[] inputData = input.split(",");
            System.out.println("inputData");
            System.out.println(inputData);

            Medication medication = new Medication();

            medication.setMedicineName();
            medication.setDescription(chat.getDescription());
            medication.setStartDate(chat.getLastName());
            medication.setDaysForEndDate(chat.getUserName());
            medication.setDaysBetweenDoses(new Timestamp(System.currentTimeMillis()));
            medication.setDosesForDay(true);

            medication.setMedicineName(inputData[0].trim());
            medication.setDescription(inputData[1].trim());
            medication.setStartDate(parseTimestamp(inputData[2], "yyyy-MM-dd", ERROR_TIMESTAMP_MASSAGE));
            medication.setDaysForEndDate(parseIntegerValue(inputData[3], ERROR_INTEGER_MASSAGE));
            medication.setDaysBetweenDoses(parseIntegerValue(inputData[4], ERROR_INTEGER_MASSAGE));
            medication.setDosesForDay(parseIntegerValue(inputData[5], ERROR_INTEGER_MASSAGE));

            medicationRepository.save(medication);
            log.info("\nMEDICATION saved: " + medication);
        }
    }
*/

//    /*метод по добавлению новых лекарст в бд*/
//    public void addMedication( Update update) {
//        //Scanner scanner = new Scanner(System.in);
//        if (update.hasCallbackQuery()) {
//            String callbackData = update.getCallbackQuery().getData();
//            long messageId = update.getCallbackQuery().getMessage().getMessageId();
//            long chatId = update.getCallbackQuery().getMessage().getChatId();
//
//            boolean validInput = false;
//            while (!validInput) {
//                prepareAndSendMessage(chatId, INFO_TEXT);
//
//                //if (!scanner.hasNextLine()) {
//                //    System.out.println("Ошибка ввода данных. Пожалуйста, введите данные в формате: название, описание, начало, конец, интервал");
//                //    return;
//                //}
//                //String inputе = scanner.nextLine();
//                //System.out.println("inputе");
//                //System.out.println(inputе);
//
//                Update userInput = UpdateUserInput(update); // Call the UpdateUserInput method and store the result
//                System.out.println("userInput");
//                System.out.println(userInput);
//
//                if (userInput != null && userInput.hasMessage() && userInput.getMessage().hasText()) {
//                    String input = userInput.getMessage().getText();
//                    System.out.println("input");
//                    System.out.println(input);
//
//                    String[] inputData = input.split(",");
//                    System.out.println("inputData");
//                    System.out.println(inputData);
//
//                    if (inputData.length != 6) {
//                        prepareAndSendMessage(chatId, ERROR_INPUT);
//                    } else {
//                        prepareAndSendMessage(chatId, "Ввод корректный");
//                    }
//
//                    Medication medication = new Medication();
//                    medication.setMedicineName(inputData[0].trim());
//                    medication.setDescription(inputData[1].trim());
//                    medication.setStartDate(parseTimestamp(inputData[2], "yyyy-MM-dd", ERROR_TIMESTAMP_MASSAGE));
//                    medication.setDaysForEndDate(parseIntegerValue(inputData[3], ERROR_INTEGER_MASSAGE));
//                    medication.setDaysBetweenDoses(parseIntegerValue(inputData[4], ERROR_INTEGER_MASSAGE));
//                    medication.setDosesForDay(parseIntegerValue(inputData[5], ERROR_INTEGER_MASSAGE));
//
//                    //medication.setUser(user);
//                    //medication.setChatId(chatId);
//
//                    System.out.println("medication");
//                    System.out.println(inputData);
//
//                    if (!validateInput(medication)) {
//                        continue;
//                    }
//
//                    medicationRepository.save(medication);
//
//                    prepareAndSendMessage(chatId, "Новое лекарство успешно добавлено.");
//                    validInput = true;
//                }
//            }
//
//            log.info("ADD NEW MEDICATION");
//        }
//
//    }

    /*public void addMedication(long ChatId, Update update) {

        Scanner scanner = new Scanner(System.in);


        boolean validInput = false;
        while (!validInput) {
            prepareAndSendMessage(ChatId, INFO_TEXT);

            Message message = update.getMessage();
            System.out.println("message");
            System.out.println(message);
            long chatId = message.getChatId();
            System.out.println("chatId");
            System.out.println(chatId);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));

            void originalMessage = UpdateUserInput(update);

            System.out.println(originalMessage.getText());

            message.setChatId(String.valueOf(ChatId))
            System.out.println("message");
            System.out.println(message.getText())
            executeMessage(message);
            System.out.println("message");
            System.out.println(message)
            Update update = getUpdateFromTelegram(); // Получение входящего сообщения из Telegra
            if (update != null && update.hasMessage() && update.getMessage().hasText()) {
                String input = update.getMessage().getText();
                System.out.println("input");
                System.out.println(input);

                String[] inputData = input.split(",");
                System.out.println("inputData");
                System.out.println(inputData);


                if (inputData.length != 6) {
                    prepareAndSendMessage(chatId, ERROR_INPUT);
                } else {
                    prepareAndSendMessage(chatId, "Ввод корректный");
                }

                Medication medication = new Medication();

                medication.setMedicineName(inputData[0].trim());
                medication.setDescription(inputData[1].trim());
                medication.setStartDate(parseTimestamp(inputData[2], "yyyy-MM-dd", ERROR_TIMESTAMP_MASSAGE));
                medication.setDaysForEndDate(parseIntegerValue(inputData[3], ERROR_INTEGER_MASSAGE));
                medication.setDaysBetweenDoses(parseIntegerValue(inputData[4], ERROR_INTEGER_MASSAGE));
                medication.setDosesForDay(parseIntegerValue(inputData[5], ERROR_INTEGER_MASSAGE));

                //medication.setUser(user);
                //medication.setChatId(chatId);

                System.out.println("medication");
                System.out.println(inputData);

                if (!validateInput(medication)) {
                    continue;
                }

                medicationRepository.save(medication);

                prepareAndSendMessage(chatId, "Новое лекарство успешно добавлено.");validInput = true;
            }
        }

        log.info("ADD NEW MEDICATION");
    }*/


//    public Update UpdateUserInput (Update update){
//        if (update.hasMessage() && update.getMessage().hasText() && !startWait) { // Переменная = false значит идет "запрос" на продолжение
//            String command = update.getMessage().getText();
//
//            if (command.equals("/add_medication")) {
//                UpdateSendMessage(update, INFO_TEXT);
//                startWait = true; // Переменная = true в этом цикле она не будет false, а значит будет считка сообщения до те пор, пока она снова не станет false
//            }
//        } else if (update.hasMessage() && update.getMessage().hasText() && startWait) {
//            UpdateSendMessage(update, "Вы ввели - " + update.getMessage().getText());
//            startWait = false; // "Считка" закончена
//        }
//        return update;
//    }
//
//
 //   //Todo
 //   private int parseIntegerValue (String value, String ERROR_INTEGER_MASSAGE){
 //       try {
 //           return Integer.parseInt(value.trim());
 //       } catch (NumberFormatException e) {
 //           log.error(ERROR_INTEGER_MASSAGE + e.getMessage());
 //           return 0;
 //       }
//
 //   }
//
 //   //Todo
 //   private Timestamp parseTimestamp (String value, String format, String ERROR_TIMESTAMP_MASSAGE){
 //       SimpleDateFormat dateFormat = new SimpleDateFormat(format);
 //       try {
 //           return new Timestamp(dateFormat.parse(value.trim()).getTime());
 //       } catch (ParseException e) {
 //           log.error(ERROR_TIMESTAMP_MASSAGE + e.getMessage());
 //           return null;
 //       }
 //   }
//
 //   /*корректный ввод*/
 //   private boolean validateInput (Medication medication){
 //       if (medication.getStartDate() == null ||
 //               medication.getMedicineName().isEmpty() ||
 //               medication.getDaysForEndDate() == 0 ||
 //               medication.getDosesForDay() == 0) {
 //           System.out.println(VALID_MESSAGE);
 //           return false;
 //       }
 //       return true;
 //   }
//
//
 //   /*создание кнопок внизу*/
 //   private ReplyKeyboardMarkup createKeyboardMarkup (String[]...buttonNames){
 //       ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
 //       List<KeyboardRow> keyboardRows = new ArrayList<>();
//
 //       for (String[] rowNames : buttonNames) {
 //           KeyboardRow row = new KeyboardRow();
 //           for (String buttonName : rowNames) {
 //               row.add(buttonName);
 //           }
 //           keyboardRows.add(row);
 //       }
//
 //       keyboardMarkup.setKeyboard(keyboardRows);
 //       return keyboardMarkup;
 //   }
//
 //   private void UpdateSendMessage (Update update, String message){
 //       SendMessage updatemessage = new SendMessage();
 //       updatemessage.setChatId(update.getMessage().getChatId().toString());
 //       updatemessage.setText(message);
//
 //       executeMessage(updatemessage);
 //   }
//
 //   /*отпрака сообщения*/
 //   private void prepareAndSendMessage ( long chatId, String textToSend){
 //       SendMessage message = new SendMessage();
 //       message.setChatId(String.valueOf(chatId));
 //       message.setText(textToSend);
//
 //       executeMessage(message);
 //   }
//
 //   /*отпрака сообщения + отображение начальных кнопок*/
 //   private void sendMessage ( long chatId, String textToSend){
//
 //       SendMessage message = new SendMessage();
 //       message.setChatId(String.valueOf(chatId));
 //       message.setText(textToSend);
//
 //       ReplyKeyboardMarkup keyboardMarkup = createKeyboardMarkup(
 //               new String[]{"Посмотр истории приёма", "Начать приём нового лекарства", "Текущий приём"},
 //               new String[]{"register", "check my data", "delete my data"}
 //       );
 //       message.setReplyMarkup(keyboardMarkup);
//
 //       executeMessage(message);
 //   }
//
//
//}
//
//
//