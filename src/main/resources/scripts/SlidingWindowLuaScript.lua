-- Sliding Window Counter Rate Limiter (using Sorted Set)
-- Stores individual request timestamps
-- Returns: [allowed (0/1), current_count, retry_after_seconds]

local key = KEYS[1]
local max_requests = tonumber(ARGV[1])
local window_size_seconds = tonumber(ARGV[2])
local current_time_millis = tonumber(ARGV[3])

local window_size_millis = window_size_seconds * 1000
local window_start = current_time_millis - window_size_millis

-- Remove all requests older than the current sliding window
redis.call('ZREMRANGEBYSCORE', key, 0, window_start)

-- Count requests in the current window
local current_count = redis.call('ZCOUNT', key, window_start, current_time_millis)

-- Check if we can allow this request
if current_count >= max_requests then
    -- Request denied - calculate retry after
    local retry_after_seconds

    -- Get the oldest request timestamp in the window
    local oldest_requests = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')

    if #oldest_requests >= 2 then
        local oldest_timestamp = tonumber(oldest_requests[2])

        -- Calculate when this oldest request will exit the window
        local oldest_expiry = oldest_timestamp + window_size_millis
        local wait_millis = oldest_expiry - current_time_millis

        retry_after_seconds = math.max(1, math.ceil(wait_millis / 1000))
    else
        -- Fallback: wait for entire window
        retry_after_seconds = window_size_seconds
    end

    return {0, current_count, retry_after_seconds}
end

-- Request allowed - add current timestamp to sorted set
redis.call('ZADD', key, current_time_millis, current_time_millis)

-- Set expiration (cleanup old keys)
redis.call('EXPIRE', key, window_size_seconds + 10)

-- Return success
return {1, current_count + 1, 0}
