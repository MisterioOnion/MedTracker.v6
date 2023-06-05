package com.example.MedTracker.serviceBot;

import com.example.MedTracker.configuration.MedTrackerBotConfiguration;
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

import java.io.Console;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MedTrackerBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;
    final MedTrackerBotConfiguration config;

    static final String HELP_TEXT =
            "This bot is created to demonstrate Spring capabilities.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /mydata to see data stored about yourself\n\n" +
            "Type /help to see this message again";
    static final String ERROR_TEXT = "ERROR occurred: ";
    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";

    public MedTrackerBot(MedTrackerBotConfiguration config) {

        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
        listOfCommands.add(new BotCommand("/deletdata", "delete my data"));
        listOfCommands.add(new BotCommand("/help", "info how use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        listOfCommands.add(new BotCommand("/register", "add new user"));

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

        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if(messageText.contains("/send") && config.getAdminId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user: users){
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            }

            else {
                // Проверка, является ли пользователь зарегистрированным
                User user = userRepository.findById(chatId).orElse(null);
                boolean isRegistration = user != null && user.isRegistration();

                if (isRegistration) {
                    switch (messageText) {
                        case "/start":
                            String text = EmojiParser.parseToUnicode("Вы уже зарегистрированы." + ":relaxed:");
                            sendMessage(chatId, text);
                            break;

                        case "/help":
                            prepareAndSendMessage(chatId, HELP_TEXT);
                            break;

                        //case "/register":
                        //    break;

                        default:
                            String texts = EmojiParser.parseToUnicode("Извините, команда пока не работает" + ":disappointed:");
                            prepareAndSendMessage(chatId, texts);
                    }
                } else {
                    switch (messageText) {
                        case "/start":
                            register(chatId);
                            break;

                        //case "/help":
                        //    prepareAndSendMessage(chatId, HELP_TEXT);
                        //    break;

                        default:
                            String texts = EmojiParser.parseToUnicode(
                                    "Извините, но вы пока не зарегистрированы" + ":disappointed:"+
                                    "\nПройдите регистрацию /start");
                            prepareAndSendMessage(chatId, texts);
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

    private void executeMessage(SendMessage message){
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }
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



    private ReplyKeyboardMarkup createKeyboardMarkup(String[]... buttonNames) {
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

    private void prepareAndSendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    private void sendMessage(long chatId, String textToSend) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = createKeyboardMarkup(
                new String[] { "Посмотр истории приёма", "Начать приём нового лекарства", "Текущий приём" },
                new String[] { "register", "check my data", "delete my data" }
        );
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }
}
