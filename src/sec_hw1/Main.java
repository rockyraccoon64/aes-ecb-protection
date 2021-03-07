package sec_hw1;

import javax.crypto.*;
import java.io.*;
import java.security.*;

/* Информационная безопасность
 * Домашняя работа №1, вариант 1
 * Сало Андрей, МЕН-472201 (МО-401) */

// TODO собрать обработку всех исключений в Main

public class Main {
    private static final int BLOCK_SIZE = 16;
    private static final File DICT_FILE = new File("Dictionary.matmex");
    private static final File TRANSLATION_FILE = new File("Translation.matmex");

    public static void main(String[] args) {
        if (args.length != 2 && !(args.length == 1 && args[0].equals("translate"))) {
            System.out.println("Error: incorrect syntax");
            return;
        }
        try {
            switch (args[0]) {
                case "prepare":
                    prepare(args[1]);
                    break;
                case "encode":
                    encode(args[1]);
                    break;
                case "translate":
                    translate();
                    break;
                case "decode":
                    decode(args[1]);
                    break;
                default:
                    System.out.println("Error: incorrect command");
                    break;
            }
        }
        catch (FileNotFoundException ex) {
            System.out.println("Error: file doesn't exist");
            ex.printStackTrace();
        }
        catch (IOException ex) {
            System.out.println("Error: I/O exception");
            ex.printStackTrace();
        } catch (NoSuchPaddingException | NoSuchAlgorithmException ex) {
            System.out.println("Error: bad at coding");
            ex.printStackTrace();
        } catch (GeneralSecurityException ex) {
            System.out.println("Error: bad at security");
            ex.printStackTrace();
        }
    }

    // Извлечение содержимого файла в виде массива байтов
    public static byte[] getBytes(File file) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            return bis.readAllBytes();
        }
    }

    // Перезапись файла выбранными байтами
    public static void writeFile(File file, byte[] bytes) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, false))) {
            bos.write(bytes);
        }
    }

    // Создание файла словаря
    public static void createDictionaryFile() throws IOException {
        byte[] bytes = new byte[256 * BLOCK_SIZE];
        for (int i = 0; i < 256; i++) {
            int start = i * BLOCK_SIZE;
            bytes[start] = (byte)i;
            for (int j = 1; j < BLOCK_SIZE; j++) {
                bytes[start + j] = 0;
            }
        }
        writeFile(DICT_FILE, bytes);
    }

    public static File openFile(String filename) throws FileNotFoundException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        return file;
    }

    // Расширяет указанный исходный файл с данными и создаёт файл словаря
    public static void prepare(String filename) throws IOException {
        File file = openFile(filename);
        byte[] bytes = getBytes(file);
        byte[] paddedBytes = new byte[bytes.length * BLOCK_SIZE];
        for (int i = 0; i < bytes.length; i++) {
            int start = i * BLOCK_SIZE;
            paddedBytes[start] = bytes[i];
            for (int j = 1; j < BLOCK_SIZE; j++) {
                paddedBytes[start + j] = 0;
            }
        }
        writeFile(file, paddedBytes);
        createDictionaryFile();
    }

    // Шифрует указанный расширенный файл и файл словаря AES со случайным ключом
    public static void encode(String filename) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        File file = openFile(filename);
        Cipher cipher = createAndInitCipher();
        encodeFile(file, cipher);
        encodeFile(DICT_FILE, cipher);
    }

    // Создание и инициализация шифра AES в режиме электронной кодовой книги
    public static Cipher createAndInitCipher() throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException {
        Cipher cipher = null;
        cipher = Cipher.getInstance("AES/ECB/NoPadding");
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256, new SecureRandom());
        SecretKey secretKey = keyGenerator.generateKey();
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher;
    }

    // Шифрование отдельного файла с помощью выбранного шифра
    public static void encodeFile(File file, Cipher cipher) throws IOException, BadPaddingException, IllegalBlockSizeException {
        byte[] bytes = getBytes(file);
        byte[] encodedBytes = cipher.doFinal(bytes);
        writeFile(file, encodedBytes);
    }

    // Выводит в отдельный файл таблицу сопоставления блоков шифртекста и блоков исходного текста, основываясь на словаре
    // Формат выходного файла: ШИФРОВАННЫЙ_БЛОК1 БЛОК1 ШИФРОВАННЫЙ_БЛОК2 БЛОК2 и т.д.
    public static void translate() throws IOException {
        byte[] bytes = getBytes(DICT_FILE);
        byte[] translationBytes = new byte[bytes.length * 2];
        for (int i = 0; i < translationBytes.length; i++) {
            translationBytes[i] = 0;
        }
        for (int i = 0; i < 256; i++) {
            int originalEncodedStart = i * BLOCK_SIZE;
            for (int j = 0; j < BLOCK_SIZE; j++) {
                int tableEncodedStart = originalEncodedStart * 2;
                int tableDecodedStart = tableEncodedStart + BLOCK_SIZE;
                translationBytes[tableEncodedStart + j] = bytes[originalEncodedStart + j];
                translationBytes[tableDecodedStart] = (byte)i;
            }
        }
        writeFile(TRANSLATION_FILE, translationBytes);
    }

    // На основании таблицы сопоставления расшифровывает наш файл с данными, после чего убирает из него расширение
    public static void decode(String filename) throws IOException {
        File file = openFile(filename);
        byte[] encodedBytes = getBytes(file);
        byte[] decodedBytes = new byte[encodedBytes.length];
    }

    public static byte[] decodeBytes(byte[] encodedBytes) throws IOException {
        byte[] translationBytes = getBytes(TRANSLATION_FILE);
        byte[] decodedBytes = new byte[encodedBytes.length];
    }

    public static byte[] removePadding(byte[] bytes) {

    }
}
