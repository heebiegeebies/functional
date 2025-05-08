package stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class ReduceTest {

    @DisplayName("숫자 더하기")
    @Test
    void reduceTest() {

        int expected = 55;
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        Integer result = numbers.stream().reduce(0, Integer::sum);

        Assertions.assertEquals(expected, result);
    }

    @DisplayName("문자열 합치기")
    @Test
    void reduceTest2() {

        String expected = "this is a sweet potato";

        List<String> list = List.of("this", "is", "a", "sweet", "potato");

        String result = list.stream().reduce("", (a, b) -> a + " " + b);
        Assertions.assertEquals(expected, result);

    }

    @DisplayName("문자열 합치기 2")
    @Test
    void reduceStringTest2() {
        String expected = "this is a potato";

        List<Potato> list = List.of(
                new Potato("this", 100),
                new Potato("is", 100),
                new Potato("a", 100),
                new Potato("potato", 100)
        );

        String actual = list.stream().map(Potato::name).reduce((a, b) -> a + " " + b).orElseThrow(NoSuchElementException::new);

        Assertions.assertEquals(expected, actual);

    }

    @DisplayName("dto에서 가장 작은 숫자 찾기")
    @Test
    void reduceTest3() {

        List<Employee> employeeList = List.of(
                new Employee("Alice", 5000),
                new Employee("Bob", 4000),
                new Employee("Charlie", 6000),
                new Employee("David", 4500));

        Employee employee = employeeList.stream()
                .reduce(BinaryOperator.maxBy(Comparator.comparingInt(Employee::getSalary)))
                .orElseThrow(() -> new NoSuchElementException("no employee"));

        Assertions.assertEquals(6000, employee.getSalary());

    }

    @DisplayName("grouping 하기")
    @Test
    void reduceGroupingExample() {

        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        NumberGroupingResult groupedResult = numbers.stream().reduce(
                new NumberGroupingResult(new ArrayList<>(), new ArrayList<>()),
                (n1, n2) -> {
                    if (n1.isEven(n2)) n1.addEven(n2);
                    else n1.addOdd(n2);
                    return n1;
                },
                (r1, r2) -> {
                    r1.evenNumbers.addAll(r2.evenNumbers);
                    r1.oddNumbers.addAll(r2.oddNumbers);
                    return r1;
                }
        );

        groupedResult.evenNumbers.forEach(System.out::println);
        groupedResult.oddNumbers.forEach(System.out::println);

        Map<Boolean, List<Integer>> partition = numbers.stream().collect(Collectors.partitioningBy(i -> i % 2 == 0));

        partition.forEach((aBoolean, integers) -> {
            System.out.println("aBoolean = " + aBoolean);
            integers.forEach(System.out::println);
        });

    }

    @DisplayName("grouping 하기 2; 감자 통계")
    @Test
    void reduceGroupingExample2() {
        List<Potato> potatoes = List.of(
                new Potato("a", 100),
                new Potato("b", 200),
                new Potato("c", 300),
                new Potato("d", 400)
        );

        PotatoStat potatoResult = potatoes.stream()
                .reduce(
                        new PotatoStat(Integer.MIN_VALUE, Integer.MAX_VALUE, 0),
                        (potatoStat, potato) -> new PotatoStat(
                                Math.max(potatoStat.high, potato.price),
                                Math.min(potatoStat.low, potato.price),
                                potatoStat.total + potato.price
                        ),
                        (stat1, stat2) -> new PotatoStat(
                                Math.max(stat1.high, stat2.high),
                                Math.min(stat1.low, stat2.low),
                                stat1.total + stat2.total
                        )
                );

        assertThat(potatoResult.total).isEqualTo(1000);
        assertThat(potatoResult.high).isEqualTo(400);
        assertThat(potatoResult.low).isEqualTo(100);

    }

    @DisplayName("collect grouping 하기 2; 감자 통계")
    @Test
    void collectGroupingExample() {
        List<Potato> potatoes = List.of(
                new Potato("a", 100),
                new Potato("b", 200),
                new Potato("c", 300),
                new Potato("d", 400)
        );

        PotatoStat collect = potatoes.stream().collect(
                () -> new PotatoStat(Integer.MIN_VALUE, Integer.MAX_VALUE, 0),
                (stat, potato) -> {
                    stat.setHigh(Math.max(stat.getHigh(), potato.price));
                    stat.setLow(Math.min(stat.getLow(), potato.price));
                    stat.setTotal(stat.total + potato.price);
                },
                (potatoStat, potatoStat2) -> {
                    potatoStat.setHigh(Math.max(potatoStat.getHigh(), potatoStat2.getHigh()));
                    potatoStat.setLow(Math.min(potatoStat.getLow(), potatoStat2.getLow()));
                    potatoStat.setTotal(potatoStat.total + potatoStat2.total);
                }
        );

        assertThat(collect.high).isEqualTo(400);
        assertThat(collect.low).isEqualTo(100);
        assertThat(collect.total).isEqualTo(1000);
    }

    @DisplayName("reduce grouping bad case :: parallel stream with mutable")
    @Test
    void reduceWithMutableCase() {
        List<Potato> potatoes = List.of(
                new Potato("a", 100),
                new Potato("b", 200),
                new Potato("c", 300),
                new Potato("d", 400)
        );

        for (int i  = 0; i < 100; i++) {
            PotatoStat result = potatoes.parallelStream() // ⬅️ using parallel!
                    .reduce(
                            new PotatoStat(0, Integer.MAX_VALUE, 0),
                            (stat, potato) -> {
                                stat.high = Math.max(stat.high, potato.price); // mutating shared object
                                stat.low = Math.min(stat.low, potato.price);
                                stat.total += potato.price;
                                return stat;
                            },
                            (s1, s2) -> s1  // ⬅️ bad combiner logic when parallel
                    );

            assertThat(result.total).isEqualTo(1000);
            assertThat(result.high).isEqualTo(400);
            assertThat(result.low).isEqualTo(100);
        }

    }

    private static class Employee {

        private final String name;
        private int salary;

        public Employee(String name, int salary) {
            this.name = name;
            this.salary = salary;
        }

        public String getName() {
            return name;
        }

        public int getSalary() {
            return salary;
        }

        public void setSalary(int salary) {
            this.salary = salary;
        }
    }

    private record Potato(String name, int price) {
    }

    private record NumberGroupingResult(List<Integer> oddNumbers, List<Integer> evenNumbers) {
        public boolean isOdd(int num) {
            return num % 2 != 0;
        }

        public boolean isEven(int num) {
            return !isOdd(num);
        }

        public void addEven(int num) {
            evenNumbers.add(num);
        }

        public void addOdd(int num) {
            oddNumbers.add(num);
        }
    }

    private static class PotatoStat {

        private int high;
        private int low;
        private int total;
        public PotatoStat(int high, int low, int total) {
            this.high = high;
            this.low = low;
            this.total = total;
        }

        public int getHigh() {
            return high;
        }

        public void setHigh(int high) {
            this.high = high;
        }

        public int getLow() {
            return low;
        }

        public void setLow(int low) {
            this.low = low;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }

}
