import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.coupon.template.entity.CouponTemplate;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Resource
    private CouponTemplateMapper couponTemplateMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/check")
    public Map<String, Object> check() {
        Map<String, Object> result = new HashMap<>();

        // MySQL：查一条模板
        CouponTemplate template = couponTemplateMapper.selectOne(
                new LambdaQueryWrapper<CouponTemplate>().last("LIMIT 1"));
        result.put("mysql", template != null ? template.getName() : "NO DATA");

        // Redis：写一个 key 再读回来
        stringRedisTemplate.opsForValue().set("health:check", "ok");
        result.put("redis", stringRedisTemplate.opsForValue().get("health:check"));

        return result;
    }
}