-- KEYS[1] = coupon:stock:{templateId}
-- 返回值：1 表示预扣成功，0 表示库存不足
local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
if stock <= 0 then
    return 0
end
redis.call('DECR', KEYS[1])
return 1