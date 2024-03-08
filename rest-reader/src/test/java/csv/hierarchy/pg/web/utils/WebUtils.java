package csv.hierarchy.pg.web.utils;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@UtilityClass
public class WebUtils {
    public <T> Page<T> paginate(List<T> list, PageRequest pageable) {
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<T> paginatedList;

        if (list.size() < startItem) {
            paginatedList = List.of();
        } else {
            int toIndex = Math.min(startItem + pageSize, list.size());
            paginatedList = list.subList(startItem, toIndex);
        }
        return new PageImpl<>(paginatedList, pageable, list.size());
    }
}
