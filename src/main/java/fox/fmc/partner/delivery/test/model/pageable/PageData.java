package fox.fmc.partner.delivery.test.model.pageable;

import lombok.Data;

@Data
public class PageData {
    int pageNumber;
    int pageSize;
    SortDetails sort;
}
