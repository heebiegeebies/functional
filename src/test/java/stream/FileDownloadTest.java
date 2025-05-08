package stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileDownloadTest {

    @BeforeAll
    static void showRuntimeInfo() {
        long maxHeapMemory = Runtime.getRuntime().maxMemory();
        System.out.println("configured heap memory = " + maxHeapMemory / FileSizeUnit.MEGA_BYTE.size + " mb");
    }

    @DisplayName("whenLazilyLoaded_thenDoesNotThrowOOM; heap/256mb, total file size = 10 * 100 = 1gb")
    @Test
    void whenLazilyLoaded_thenDoesNotThrowOOM() {
        Assertions.assertDoesNotThrow(() -> {
            List<FileDto> fileDtos = getFileDtos();
            File root = new File("output.zip");
            root.deleteOnExit();
            String testFilename = "eager-allocation";
            createFile(testFilename, 10, FileSizeUnit.MEGA_BYTE);
            try (FileOutputStream fos = new FileOutputStream(root)) {
                ZipOutputStream zos = new ZipOutputStream(fos);
                for (FileDto fileDto : fileDtos) {
                    try (FileInputStream fis = new FileInputStream(testFilename)) {
                        ZipEntry zipEntry = new ZipEntry(fileDto.name());
                        zos.putNextEntry(zipEntry);

                        zos.write(fis.readAllBytes());
                        zos.closeEntry();
                    }
                }
                zos.finish();
            }
        });
    }

    @DisplayName("whenEagerlyLoaded_thenThrowsOOM; heap/256mb, loading/500 mb")
    @Test
    void whenEagerlyLoaded_thenThrowsOOM() {

        Assertions.assertThrows(OutOfMemoryError.class, () -> {
            List<FileDto> fileDtos = getFileDtos();

            // eager allocation
            List<byte[]> eagerlyLoadedByteArrayList = fileDtos.stream()
                    .map(fileDto -> {
                        int fileSize = fileDto.unit().size * 5; // 5 * 100 = 500 mb
                        return new byte[fileSize];
                    })
                    .toList();
        });
    }

    @DisplayName("whenLazilyLoadedWithAnonymousClass_thenDoesNotThrows; ")
    @Test
    void whenLazilyLoadedWithAnonymousClass_thenDoesNotThrows() {
        Assertions.assertDoesNotThrow(() -> {
                    List<FileDto> fileDtos = getFileDtos();

                    List<Supplier<byte[]>> byteArraySupplierList = fileDtos.stream()
                            .<Supplier<byte[]>>map(fileDto -> () -> new byte[fileDto.unit().size * 5])
                            .toList();

                    try (FileOutputStream fos = new FileOutputStream("lazy-allocation")) {
                        for (Supplier<byte[]> supplier : byteArraySupplierList) {
                            byte[] bytes = supplier.get();
                            fos.write(bytes);
                        }
                    } catch (Exception e2) {
                        throw new RuntimeException(e2);
                    }
                }
        );

    }

    private static List<FileDto> getFileDtos(int count, FileSizeUnit unit, String fileName) {
        return IntStream.range(1, count)
                .mapToObj(
                        i -> FileDto.builder()
                                .name(fileName + " " + i)
                                .unit(unit)
                                .build()
                )
                .toList(); // approx
    }

    private static List<FileDto> getFileDtos() {
        return getFileDtos(100, FileSizeUnit.MEGA_BYTE, "test");
    }

    private static void createFile(String filename, int size, FileSizeUnit unit) {
        File dummy = new File(filename);
        dummy.deleteOnExit();
        try (FileOutputStream test = new FileOutputStream(dummy)) {
            byte[] bytes = new byte[size * unit.size];
            test.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createFile() {
        createFile("test", 10, FileSizeUnit.MEGA_BYTE);
    }

    @Builder
    private record FileDto(String name, FileSizeUnit unit) {
    }

    @Getter
    @RequiredArgsConstructor
    private enum FileSizeUnit {
        MEGA_BYTE(1024 * 1024),
        GIGA_BYTE(1024 * 1024 * 1024),
        ;
        final int size;
    }
}
