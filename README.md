# Redis-searches

# Command to start redis-stack
docker run -v "C:\Users\ `<YourUser>` \code\infra\redis\data:/data/" -d -p 6379:6379 redis/redis-stack:7.2.0-v10

# Create number of fake names to search
curl -k -X POST http://localhost:8080/names-searches?numberOfNames=1000

# Search by name
curl -k -X GET http://localhost:8080/names-searches?name=felipe