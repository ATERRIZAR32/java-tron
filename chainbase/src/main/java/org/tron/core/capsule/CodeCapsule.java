/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol;

@Slf4j(topic = "capsule")
public class CodeCapsule implements ProtoCapsule<Protocol.ByteArray> {

  private Protocol.ByteArray code;

  public CodeCapsule(byte[] code) {
    this.code = Protocol.ByteArray.newBuilder()
        .setData(ByteString.copyFrom(code))
        .build();
  }

  public Sha256Hash getCodeHash() {
    return Sha256Hash.of(CommonParameter.getInstance().isECKeyCryptoEngine(),
        this.code.getData().toByteArray());
  }

  @Override
  public byte[] getData() {
    return this.code.getData().toByteArray();
  }

  @Override
  public Protocol.ByteArray getInstance() {
    return this.code;
  }

  @Override
  public String toString() {
    return Arrays.toString(this.code.getData().toByteArray());
  }
}
