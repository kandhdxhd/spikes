package x.loggy;

import lombok.Getter;
import lombok.ToString;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;

@FeignClient(name = "happy-path", url = "https://httpbin.org/get")
public interface SampleHttpBin {
    @GetMapping(produces = "application/json")
    Response get();

    @Getter
    @ToString
    class Response {
        URI url;
    }
}
