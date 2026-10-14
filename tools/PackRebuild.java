import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 与 PayloadManager.decryptPack/parsePlainData 严格互逆的 pack 重建工具。
 * 用法: java PackRebuild.java <in.pack> <newPayload> <newRoot> <out.pack>
 * 格式: 外 [8B MAGIC][16B IV][AES-256-CBC( 内 [8B MAGIC][int32 count LE]
 *        per: [int32 nameLen LE][name][int64 dataLen LE][data] ) ]
 */
public class PackRebuild {
    static final byte[] MAGIC = "S9ROOTPK".getBytes(StandardCharsets.US_ASCII);
    static final String KEY_TEXT = "Samsung-root-kit-payloads-pack-2026";

    static byte[] sha256(byte[] b) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(b);
    }
    static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format("%02x", x));
        return s.toString();
    }
    static SecretKeySpec key() throws Exception {
        return new SecretKeySpec(sha256(KEY_TEXT.getBytes(StandardCharsets.UTF_8)), "AES");
    }

    static byte[] decryptOuter(byte[] pack) throws Exception {
        for (int i = 0; i < 8; i++) if (pack[i] != MAGIC[i]) throw new RuntimeException("bad outer magic");
        byte[] iv = Arrays.copyOfRange(pack, 8, 24);
        byte[] ct = Arrays.copyOfRange(pack, 24, pack.length);
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, key(), new IvParameterSpec(iv));
        return c.doFinal(ct);
    }

    static LinkedHashMap<String, byte[]> parse(byte[] plain) {
        ByteBuffer b = ByteBuffer.wrap(plain).order(ByteOrder.LITTLE_ENDIAN);
        byte[] m = new byte[8]; b.get(m);
        if (!Arrays.equals(m, MAGIC)) throw new RuntimeException("bad inner magic");
        int n = b.getInt();
        if (n < 0 || n > 1000) throw new RuntimeException("bad count " + n);
        LinkedHashMap<String, byte[]> map = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int nl = b.getInt();
            byte[] nb = new byte[nl]; b.get(nb);
            String name = new String(nb, StandardCharsets.UTF_8);
            long dl = b.getLong();
            byte[] data = new byte[(int) dl]; b.get(data);
            map.put(name, data);
        }
        return map;
    }

    static byte[] serialize(LinkedHashMap<String, byte[]> map) {
        int size = 8 + 4;
        for (var e : map.entrySet()) {
            byte[] nb = e.getKey().getBytes(StandardCharsets.UTF_8);
            size += 4 + nb.length + 8 + e.getValue().length;
        }
        ByteBuffer b = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        b.put(MAGIC); b.putInt(map.size());
        for (var e : map.entrySet()) {
            byte[] nb = e.getKey().getBytes(StandardCharsets.UTF_8);
            b.putInt(nb.length); b.put(nb);
            b.putLong(e.getValue().length); b.put(e.getValue());
        }
        return b.array();
    }

    static byte[] encryptOuter(byte[] plain) throws Exception {
        byte[] iv = new byte[16]; new SecureRandom().nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, key(), new IvParameterSpec(iv));
        byte[] ct = c.doFinal(plain);
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(MAGIC); o.write(iv); o.write(ct);
        return o.toByteArray();
    }

    public static void main(String[] a) throws Exception {
        byte[] pack = Files.readAllBytes(Paths.get(a[0]));
        LinkedHashMap<String, byte[]> orig = parse(decryptOuter(pack));
        System.out.println("old entries: " + orig.size());

        String pName = "S9180_S918U1UES8FZG1__payload";
        String rName = "S9180_S918U1UES8FZG1__root";
        byte[] pd = Files.readAllBytes(Paths.get(a[1]));
        byte[] rd = Files.readAllBytes(Paths.get(a[2]));

        LinkedHashMap<String, byte[]> map = new LinkedHashMap<>(orig);
        map.put(pName, pd);
        map.put(rName, rd);

        byte[] out = encryptOuter(serialize(map));
        Files.write(Paths.get(a[3]), out);
        System.out.println("new entries: " + map.size());
        System.out.println("new pack size: " + out.length);
        System.out.println("new pack sha256: " + hex(sha256(out)));

        // 往返自校验
        LinkedHashMap<String, byte[]> v = parse(decryptOuter(out));
        System.out.println("roundtrip entries: " + v.size());
        System.out.println("payload ok=" + v.containsKey(pName) + " sha=" + hex(sha256(v.get(pName))));
        System.out.println("root    ok=" + v.containsKey(rName) + " sha=" + hex(sha256(v.get(rName))));
        int mismatch = 0;
        for (var e : orig.entrySet()) {
            byte[] nv = v.get(e.getKey());
            if (nv == null || !Arrays.equals(nv, e.getValue())) mismatch++;
        }
        System.out.println("original entries mismatches: " + mismatch);
    }
}
