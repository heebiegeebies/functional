package stream;

import lombok.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class MapCollectTest {

    private static List<Product> productList;

    @BeforeAll
    static void setup() {
        productList = List.of(
                Product.builder().id("A-1").producedAt("20240101").price(1500).build(),
                Product.builder().id("B-1").producedAt("20250101").price(3000).build(),
                Product.builder().id("C-1").producedAt("20230101").price(4000).build(),
                Product.builder().id("A-2").producedAt("20230101").price(2000).build(),
                Product.builder().id("D-1").producedAt("20230101").price(3000).build(),
                Product.builder().id("A-3").producedAt("20240101").price(1000).build()
        );

    }

    @Test
    void mapCollectTest() {

        Map<String, List<Product>> result = productList.stream().collect(Collectors.groupingBy(p -> String.valueOf(p.id().charAt(0))));

        assertThat(result).containsKeys("A", "B", "C", "D");

        // Assert group sizes
        assertThat(result.get("A")).hasSize(3);
        assertThat(result.get("B")).hasSize(1);
        assertThat(result.get("C")).hasSize(1);
        assertThat(result.get("D")).hasSize(1);

    }

    @Test
    void collectCustomStatus() {
        ProductStat result = productList.stream().collect(
                () -> new ProductStat(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0),
                (productStat, product) -> {
                    productStat.setMax(Math.max(productStat.getMax(), product.price()));
                    productStat.setMin(Math.min(productStat.getMin(), product.price()));
                    productStat.setSum(productStat.getSum() + product.price());
                    productStat.count();
                }, (stat1, stat2) -> {
                    stat1.setMax(Math.max(stat1.getMax(), stat2.getMax()));
                    stat1.setMin(Math.min(stat1.getMin(), stat2.getMin()));
                    stat1.setSum(stat1.getSum() + stat2.getSum());
                    stat1.setCount(stat1.getCount() + stat2.getCount());
                });

        IntSummaryStatistics collect = productList.stream().collect(Collectors.summarizingInt(Product::price));
        double average = collect.getAverage();
        int max = collect.getMax();
        int min = collect.getMin();
        long sum = collect.getSum();

        assertThat(result.getMax()).isEqualTo(max);
        assertThat(result.getMin()).isEqualTo(min);
        assertThat(result.getAverage()).isEqualTo(average);
        assertThat(result.getSum()).isEqualTo(sum);

    }

    @Builder
    private record Product(String id, String producedAt, int price) {
    }

    @ToString
    @Builder
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class ProductStat {
        private int max;
        private int min;
        private long sum;
        private int count;

        public void count() {
            count++;
        }

        public double getAverage() {
            return (double) sum / count;
        }
    }

}
