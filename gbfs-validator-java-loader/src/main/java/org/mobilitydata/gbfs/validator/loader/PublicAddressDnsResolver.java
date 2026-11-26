/*
 *
 *  *
 *  *
 *  *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  *  * You may not use this work except in compliance with the Licence.
 *  *  * You may obtain a copy of the Licence at:
 *  *  *
 *  *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the Licence for the specific language governing permissions and
 *  *  * limitations under the Licence.
 *  *
 *
 */

package org.mobilitydata.gbfs.validator.loader;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;

/**
 * DNS resolver that only returns public internet addresses.
 *
 * <p>Used by {@link Loader} when it fetches URLs supplied by untrusted callers (the HTTP API):
 * a feed URL, every URL listed in its discovery file, the OAuth token URL and every redirect
 * target are resolved here, and the connection is made to the address checked here. A host
 * resolving to a loopback, private, link-local (cloud metadata), carrier-grade NAT, multicast
 * or reserved address is rejected, so the validator cannot be used to reach internal services.
 */
public class PublicAddressDnsResolver implements DnsResolver {

  private final DnsResolver delegate;

  public PublicAddressDnsResolver() {
    this(SystemDefaultDnsResolver.INSTANCE);
  }

  PublicAddressDnsResolver(DnsResolver delegate) {
    this.delegate = delegate;
  }

  @Override
  public InetAddress[] resolve(String host) throws UnknownHostException {
    InetAddress[] addresses = delegate.resolve(host);
    for (InetAddress address : addresses) {
      if (!isPublic(address)) {
        throw new UnknownHostException(
          host + " resolves to a non-public address, which is not allowed"
        );
      }
    }
    return addresses;
  }

  @Override
  public String resolveCanonicalHostname(String host)
    throws UnknownHostException {
    return delegate.resolveCanonicalHostname(host);
  }

  /**
   * Whether the address is a public internet address.
   *
   * @param address the resolved address
   * @return false for loopback, wildcard, private, link-local, multicast, carrier-grade NAT,
   *     benchmarking, documentation, IETF-reserved and IPv6 unique local addresses
   */
  public static boolean isPublic(InetAddress address) {
    if (
      address.isAnyLocalAddress() ||
      address.isLoopbackAddress() ||
      address.isLinkLocalAddress() ||
      address.isSiteLocalAddress() ||
      address.isMulticastAddress()
    ) {
      return false;
    }
    byte[] bytes = address.getAddress();
    if (address instanceof Inet4Address) {
      int a = bytes[0] & 0xff;
      int b = bytes[1] & 0xff;
      int c = bytes[2] & 0xff;
      return !(
        a == 0 || // "this" network
        (a == 100 && b >= 64 && b <= 127) || // carrier-grade NAT
        (a == 192 && b == 0 && c == 0) || // IETF protocol assignments
        (a == 192 && b == 0 && c == 2) || // documentation (TEST-NET-1)
        (a == 198 && (b == 18 || b == 19)) || // benchmarking
        (a == 198 && b == 51 && c == 100) || // documentation (TEST-NET-2)
        (a == 203 && b == 0 && c == 113) || // documentation (TEST-NET-3)
        a >= 240 // reserved and broadcast
      );
    }
    // IPv6 unique local addresses (fc00::/7). IPv4-mapped addresses are returned as Inet4Address.
    return (bytes[0] & 0xfe) != 0xfc;
  }
}
