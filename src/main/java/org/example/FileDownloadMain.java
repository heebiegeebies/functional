package org.example;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.IntStream;

public class FileDownloadMain {

    public static void main(String[] args) {


    }

    private static List<FileDto> getFileDtos() {
        return IntStream.range(1, 100).mapToObj(i -> FileDto.builder().name("test " + i).build()).toList(); // approx 2gb
    }

    @Getter
    @Builder
    @AllArgsConstructor
    private static class FileDto {
        private static final String FILE_PATH = "C:/Users/laon/Downloads/test.txt";
        private final String name;
        private final String url = FILE_PATH;
    }

}
