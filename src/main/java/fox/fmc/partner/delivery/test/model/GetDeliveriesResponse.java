package fox.fmc.partner.delivery.test.model;

import lombok.Data;

@Data
public class GetDeliveriesResponse {

    private Long took;
    private Boolean timed_out;
    private Shards _shards;
    private Hits hits;

    @Data
    public static class Shards {
        private Integer total;
        private Integer successful;
        private Integer skipped;
        private Integer failed;
    }

    @Data
    public static class Hits {
        private Total total;
        private Float max_score;
        private Hit[] hits;
    }

    @Data
    public static class Total {
        private Integer value;
        private String relation;
    }

    @Data
    public static class Hit {
        private String _index;
        private String _type;
        private String _id;
        private Float _score;
        private Source _source;
    }

    @Data
    public static class Source {
        private String batchId;
        private String status;
        private String createdAt;
        private String updatedAt;
        private String completedAt;
    }


}
