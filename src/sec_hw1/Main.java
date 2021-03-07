package sec_hw1;

import javax.crypto.*;
import java.io.*;
import java.security.*;

/* Информационная безопасность
 * Домашняя работа №1, вариант 1
 * Сало Андрей, МЕН-472201 (МО-401) */

// TODO собрать обработку всех исключений в Main

public class Main {
    private static final File DICT_FILE = new File("Dictionary.matmex");
    private static final int BLOCK_SIZE = 16;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Error: incorrect syntax");
            return;
        }
        File file = new File(args[1]);
        if (!file.exists()) {
            System.out.println("Error: file doesn't exist");
            return;
        }
        try {
            switch (args[0]) {
                case "prepare":
                    prepare(file);
                    break;
                case "encode":
                    encode(file);
                    break;
                case "translate":
                    translate(file);
                    break;
                case "decode":
                    decode(file);
                    break;
                default:
                    System.out.println("Error: incorrect command");
                    break;
            }
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

    // Расширяет указанный исходный файл с данными и создаёт файл словаря
    public static void prepare(File file) throws IOException {
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
    public static void encode(File file) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
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
    public static void translate(File file) {
        byte[] bytes = getBytes(DICT_FILE);

    }

    // На основании таблицы сопоставления расшифровывает наш файл с данными, после чего убирает из него расширение
    public static void decode(File file) throws IOException {

    }
}
