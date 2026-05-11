package com.priz.base.infrastructure.security.apikey;

import org.springframework.stereotype.Component;

import java.net.InetAddress;

/**
 * Kiểm tra client IP có nằm trong danh sách IP/CIDR cho phép không.
 * allowIps là chuỗi phân cách bởi dấu phẩy, mỗi entry là IP hoặc CIDR (vd: "192.168.1.0/24,10.0.0.1").
 */
@Component
public class IpWhitelistValidator {

    public boolean isAllowed(String clientIp, String allowIps) {
        if (allowIps == null || allowIps.isBlank()) {
            return true;
        }
        String[] entries = allowIps.split(",");
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.contains("/")) {
                if (matchesCidr(clientIp, trimmed)) return true;
            } else {
                if (trimmed.equals(clientIp)) return true;
            }
        }
        return false;
    }

    private boolean matchesCidr(String clientIp, String cidr) {
        try {
            String[] parts = cidr.split("/");
            int prefixLen = Integer.parseInt(parts[1]);
            InetAddress network = InetAddress.getByName(parts[0]);
            InetAddress client = InetAddress.getByName(clientIp);

            byte[] netBytes = network.getAddress();
            byte[] clientBytes = client.getAddress();
            if (netBytes.length != clientBytes.length) return false;

            int fullBytes = prefixLen / 8;
            int remainBits = prefixLen % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (netBytes[i] != clientBytes[i]) return false;
            }
            if (remainBits > 0 && fullBytes < netBytes.length) {
                int mask = (0xFF << (8 - remainBits)) & 0xFF;
                if ((netBytes[fullBytes] & mask) != (clientBytes[fullBytes] & mask)) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
