public static UUID toUUID(String username){
        ByteBuffer buffer=ByteBuffer.wrap(Arrays.copyOf(username.getBytes(StandardCharsets.US_ASCII),16));
        return new UUID(buffer.getLong(),buffer.getLong()); // MOST, LEAST
        }

public static String toUsername(UUID uuid){
        byte[]bytes=ByteBuffer.allocate(16).
        putLong(uuid.getMostSignificantBits()).
        putLong(uuid.getLeastSignificantBits()).array();

        // Find username end
        int length=0;
        while(length<bytes.length&&bytes[length]!=0){
        length++;
        }

        // Decode and verify
        return VerifyHelper.verifyUsername(new String(bytes,0,length,StandardCharsets.US_ASCII));
        }
