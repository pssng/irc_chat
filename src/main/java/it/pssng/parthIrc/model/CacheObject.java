package it.pssng.parthIrc.model;

import java.io.*;
import java.nio.charset.StandardCharsets;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CacheObject implements Serializable {
    private static final long serialVersionUID = 1L;
    private byte[] readerContent;
    private byte[] writerContent;

    public CacheObject(BufferedReader reader, PrintWriter writer) throws IOException {
        this.readerContent = convertReaderToByteArray(reader);
        this.writerContent = convertWriterToByteArray(writer);
    }

    private byte[] convertReaderToByteArray(BufferedReader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        return content.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] convertWriterToByteArray(PrintWriter writer) throws IOException {
        writer.flush(); // Ensure all data is written
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintWriter tempWriter = new PrintWriter(byteArrayOutputStream, true, StandardCharsets.UTF_8);
        tempWriter.print(writer.toString());
        tempWriter.close();
        return byteArrayOutputStream.toByteArray();
    }

    public BufferedReader getReader() {
        return new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(readerContent), StandardCharsets.UTF_8));
    }

    public PrintWriter getWriter() {
        return new PrintWriter(new OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8), true);
    }
}
