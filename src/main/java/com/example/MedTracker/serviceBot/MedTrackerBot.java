package com.example.MedTracker.serviceBot;

import java.sql.*;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
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
            "- если курс препарата будет проводиться повторно введите true и количество дней через " +
            "которое следует начать повторный приём," +
            " в противном случае напишите false, а в количестве дней поставьте 0\n" +
            "- также укажите какое количество раз в день вы будете принимать препарат.\n\n" +
            "Пример ввода:\n" +
            "Препарат A, краткое описание, 2023-06-08, 7, true, 10, 3\n" +
            "Препарат A, краткое описание, 2023-06-08, 7, false, 0, 3";

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

    private static Connection connection;


    private boolean addFlag = false;

    //@Autowired
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
                            //-!
                            this.addFlag = true;
                            prepareAndSendMessage(chatId, ADD_MEDICATION_TEXT);
                            //    InputMessage(messageText, chatId);// Обработка полученного сообщения
                            //    sendAddMedicationMessage(chatId);
                            //    addMedication(update);
                            break;

                        case "/register":
                            String text_ = EmojiParser.parseToUnicode(
                                    "Вы уже зарегистрированы." + ":relaxed:"
                            );
                            sendMessage(chatId, text_);
                            break;

                        default:

                            //-!
                            sendMessage(chatId, String.valueOf(addFlag));
                            if(!this.addFlag) {
                                String texts = EmojiParser.parseToUnicode(
                                        "Извините, команда пока не работает" + ":disappointed:"
                                );
                                prepareAndSendMessage(chatId, texts);
                            }
                            else {
                                InputMessage(messageText, chatId);
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

    /* Создание SQL-запроса для вставки новой записи в базу данных */
    protected void addMedication(long chatId, String name, String description, String startDate, int duration, boolean isRepeated, int daysBetweenDoses, int frequency) {
        try {
            Medication medication = new Medication();
            User user = userRepository.findById(chatId).orElse(null);

            medication.setUser(user);
            //medication.setUserId(chatId);
            medication.setId(chatId);
            medication.setMedicineName(name);
            medication.setDescription(description);
            //medication.setStartDate(Timestamp.valueOf(startDate));
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Timestamp timestampDate = new Timestamp ((dateFormat.parse(startDate)).getTime());
            medication.setStartDate(timestampDate);
            medication.setDaysForEndDate(duration);
            medication.setIsRepeated(isRepeated);
            medication.setDaysBetweenDoses(daysBetweenDoses);
            medication.setDosesForDay(frequency);
            medicationRepository.save(medication);
        } catch (Exception e) {
            System.out.println("CATCH");
            System.out.println(chatId);
            sendMessage(chatId, e.getMessage());
            e.printStackTrace();
        }
    }

    private void processMessage(String messageText, long chatId) {
        // Ваш код для обработки сообщения от пользователя
        // Например, отправка ответного сообщения
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Вы сказали: " + messageText);

        executeMessage(message);
    }

    private void InputMessage(String messageText, long chatId) {

        //-!
        sendMessage(chatId, "I AM IN <InputMessage> METHOD");
        SendMessage message = new SendMessage();

        message.setChatId(String.valueOf(chatId));
        System.out.println("InputMessage message");
        System.out.println(message);

        String[] data = messageText.split(",");
        //if (data.length != 6) {
        //    sendMessage(chatId, INFO_TEXT);
        //    return;
        //}

        String name = data[0].trim();
        String description = data[1].trim();
        String startDate = data[2].trim();
        int duration = Integer.parseInt(data[3].trim());
        boolean isRepeated = Boolean.parseBoolean(data[4].trim());
        int daysBetweenDoses = Integer.parseInt(data[5].trim());
        int frequency = Integer.parseInt(data[6].trim());
        //sendMessage(chatId, "BEFORE ADDING");
        // Добавление данных в базу данных
        addMedication(chatId, name, description, startDate, duration, isRepeated, daysBetweenDoses, frequency);

        // Отправка подтверждения пользователю
        sendMessage(chatId, "Данные успешно добавлены в базу данных.");
        
        this.addFlag = false;

        //message.setChatId(String.valueOf(chatId));
        //message.setText("Вы сказали: " + messageText);
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

    /*проверка ошибок для callback кнопок + текст*/
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
        InlineKeyboardMarkup markupInLine = createInlineKeyboardMarkup(
                "Да", YES_BUTTON,
                "Нет", NO_BUTTON
        );
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    /*создание начальных кнопок под сообщением в чате*/
    private InlineKeyboardMarkup createInlineKeyboardMarkup(String buttonText1, String callbackData1, String buttonText2, String callbackData2) {
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(buttonText1);
        button1.setCallbackData(callbackData1);

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(buttonText2);
        button2.setCallbackData(callbackData2);

        rowInLine.add(button1);
        rowInLine.add(button2);
        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        return markupInLine;
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


    /*отпрака сообщения*/
    private void prepareAndSendMessage ( long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        executeMessage(message);
    }

    /*отпрака сообщения + отображение начальных кнопок*/
    private void sendMessage ( long chatId, String textToSend){

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = createKeyboardMarkup(
                new String[]{"Посмотр истории приёма", "Начать приём нового лекарства", "Текущий приём"},
                new String[]{"register", "check my data", "delete my data"}
        );
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }


}


