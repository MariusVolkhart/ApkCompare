import okio.ByteString;
import okio.Okio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class App {

    private final String expectedPath;
    private final String actualPath;

    private App(String expectedPath, String actualPath) {
        this.expectedPath = expectedPath;
        this.actualPath = actualPath;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            throw new IllegalArgumentException("Need to provide 2 file locations");
        }
        new App(args[0], args[1]).run();
    }

    private void run() throws IOException {
        Map<String, ByteString> expectedHashes = new LinkedHashMap<>();
        Map<String, ByteString> actualHashes = new LinkedHashMap<>();
        ZipFile expected = new ZipFile(expectedPath);
        ZipFile actual = new ZipFile(actualPath);

        extractHashes(expected, expectedHashes);
        extractHashes(actual, actualHashes);

        int exitCode = 0;
        for (Map.Entry<String, ByteString> entry : expectedHashes.entrySet()) {
            ByteString actualHash = actualHashes.remove(entry.getKey());
            if (actualHash == null) {
                exitCode = 1;
                System.out.println("Actual archive did not include file " + entry.getKey());
            } else {
                if (!entry.getValue().equals(actualHash)) {
                    System.out.println(
                            String.format("Hashcode %s expected for file %s, but was %s.", entry.getValue().hex(), entry.getKey(), actualHash.hex()));
                    exitCode = 1;
                }
            }
        }
        for (String name : actualHashes.keySet()) {
            System.out.println("Encountered unexpected file in the actual archive: " + name);
            exitCode = 1;
        }

        System.exit(exitCode);
    }

    private static void extractHashes(ZipFile from, Map<String, ByteString> target) throws IOException {
        Enumeration<? extends ZipEntry> expectedEntries = from.entries();
        while(expectedEntries.hasMoreElements()) {
            ZipEntry entry = expectedEntries.nextElement();
            InputStream stream = from.getInputStream(entry);
            ByteString hash = Okio.buffer(Okio.source(stream)).readByteString().sha256();
            target.put(entry.getName(), hash);
        }
    }
}
