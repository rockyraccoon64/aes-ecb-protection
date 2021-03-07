package sec_hw1;

import javax.crypto.*;
import java.io.*;
import java.security.*;
import java.util.Arrays;
import java.util.HashMap;

/* Информационная безопасность
 * Домашняя работа №1, вариант 1
 * Сало Андрей, МЕН-472201 (МО-401) */

public class Main {
    private static final int BLOCK_SIZE = 16;
    private static final int DICT_SIZE = 256;
    private static final File DICT_FILE = new File("Dictionary.matmex");
    private static final File TRANSLATION_FILE = new File("Translation.matmex");

    // Блок из нескольких байтов
    private static class Block {
        public byte[] m_bytes;

        Block(byte[] bytes) {
            m_bytes = bytes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Block block = (Block) o;
            return Arrays.equals(m_bytes, block.m_bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(m_bytes);
        }
    }

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
        } catch (IOException ex) {
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

    // Расширяет указанный исходный файл с данными и создаёт файл словаря
    public static void prepare(String filename) throws IOException {
        File file = openFile(filename);
        byte[] bytes = getBytes(file);
        Block[] paddedBlocks = padToBlocks(bytes);
        writeFile(file, blocksToBytes(paddedBlocks));
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

    // Выводит в отдельный файл таблицу сопоставления блоков шифртекста и блоков исходного текста, основываясь на словаре
    // Формат выходного файла: ШИФРОВАННЫЙ_БЛОК1 БЛОК1 ШИФРОВАННЫЙ_БЛОК2 БЛОК2 и т.д.
    public static void translate() throws IOException {
        Block[] encodedDictBlocks = getBlocksFromFile(DICT_FILE);
        Block[] originalDictBlocks = getDictionaryBlocks();
        Block[] translationBlocks = new Block[encodedDictBlocks.length + originalDictBlocks.length];
        for (int i = 0; i < encodedDictBlocks.length; i++) {
            translationBlocks[i*2] = encodedDictBlocks[i];
            translationBlocks[i*2+1] = originalDictBlocks[i];
        }
        writeFile(TRANSLATION_FILE, blocksToBytes(translationBlocks));
    }

    // На основании таблицы сопоставления расшифровывает наш файл с данными, после чего убирает из него расширение
    public static void decode(String filename) throws IOException {
        File file = openFile(filename);
        Block[] encodedBlocks = getBlocksFromFile(file);
        Block[] translationBlocks = getBlocksFromFile(TRANSLATION_FILE);

        HashMap<Block, Block> translationMap = makeTranslationMap(translationBlocks);
        Block[] decodedBlocks = new Block[encodedBlocks.length];
        for (int i = 0; i < encodedBlocks.length; i++) {
            decodedBlocks[i] = translationMap.get(encodedBlocks[i]);
        }
        byte[] decodedBytes = blocksToBytes(decodedBlocks);
        byte[] originalBytes = removePadding(decodedBytes);
        writeFile(file, originalBytes);
    }

    // Расширить каждый байт до блока размером BLOCK_SIZE байтов
    public static Block[] padToBlocks(byte[] bytes) {
        Block[] blocks = new Block[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byte[] blockBytes = new byte[BLOCK_SIZE];
            blockBytes[0] = bytes[i];
            blocks[i] = new Block(blockBytes);
        }
        return blocks;
    }

    // Создание файла словаря
    public static void createDictionaryFile() throws IOException {
        Block[] blocks = getDictionaryBlocks();
        writeFile(DICT_FILE, blocksToBytes(blocks));
    }

    // Получить стандартное содержимое файла словаря в виде блоков
    public static Block[] getDictionaryBlocks() {
        byte[] bytes = new byte[DICT_SIZE];
        for (int i = 0; i < DICT_SIZE; i++) {
            bytes[i] = (byte)i;
        }
        return padToBlocks(bytes);
    }

    // Создание и инициализация шифра AES в режиме электронной кодовой книги
    public static Cipher createAndInitCipher() throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256, new SecureRandom());
        SecretKey secretKey = keyGenerator.generateKey();
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher;
    }

    // Шифрование отдельного файла с помощью выбранного шифра
    public static void encodeFile(File file, Cipher cipher) throws IOException,
            BadPaddingException, IllegalBlockSizeException {
        byte[] bytes = getBytes(file);
        byte[] encodedBytes = cipher.doFinal(bytes);
        writeFile(file, encodedBytes);
    }

    // Восстановить таблицу сопоставления из файла на основе HashMap
    public static HashMap<Block, Block> makeTranslationMap(Block[] translationBlocks) {
        HashMap<Block, Block> translationMap = new HashMap<>();
        for (int i = 0; i < translationBlocks.length; i += 2) {
            translationMap.put(translationBlocks[i], translationBlocks[i+1]);
        }
        return translationMap;
    }

    // Убрать расширение
    public static byte[] removePadding(byte[] paddedBytes) {
        byte[] bytes = new byte[paddedBytes.length / BLOCK_SIZE];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = paddedBytes[i * BLOCK_SIZE];
        }
        return bytes;
    }

    // Проверить наличие файла и открыть его
    public static File openFile(String filename) throws FileNotFoundException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        return file;
    }

    // Извлечение содержимого файла в виде массива байтов
    public static byte[] getBytes(File file) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            return bis.readAllBytes();
        }
    }

    // Извлечь данные из файла в виде массива блоков
    public static Block[] getBlocksFromFile(File file) throws IOException {
        byte[] bytes = getBytes(file);
        return bytesToBlocks(bytes);
    }

    // Перезапись файла выбранными байтами
    public static void writeFile(File file, byte[] bytes) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, false))) {
            bos.write(bytes);
        }
    }

    // Преобразовать массив блоков в массив байтов
    public static byte[] blocksToBytes(Block[] blocks) {
        byte[] bytes = new byte[blocks.length * BLOCK_SIZE];
        for (int i = 0; i < blocks.length; i++) {
            int start = i * BLOCK_SIZE;
            System.arraycopy(blocks[i].m_bytes, 0, bytes, start, BLOCK_SIZE);
        }
        return bytes;
    }

    // Преобразовать массив байтов в массив блоков
    public static Block[] bytesToBlocks(byte[] bytes) throws IOException {
        if (bytes.length % BLOCK_SIZE != 0) {
            throw new IOException();
        }
        int numBlocks = bytes.length / BLOCK_SIZE;
        Block[] blocks = new Block[numBlocks];
        for (int i = 0; i < numBlocks; i++) {
            int start = i * BLOCK_SIZE;
            int end = start + BLOCK_SIZE;
            byte[] range = Arrays.copyOfRange(bytes, start, end);
            blocks[i] = new Block(range);
        }
        return blocks;
    }
}
