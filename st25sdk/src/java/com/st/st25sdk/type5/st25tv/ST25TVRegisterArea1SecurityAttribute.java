/*
 * @author STMicroelectronics MMY Application team
 *
 ******************************************************************************
 * @attention
 *
 * <h2><center>&copy; COPYRIGHT 2017 STMicroelectronics</center></h2>
 *
 * Licensed under ST MIX_MYLIBERTY SOFTWARE LICENSE AGREEMENT (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *        http://www.st.com/Mix_MyLiberty
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied,
 * AND SPECIFICALLY DISCLAIMING THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************
 */

package com.st.st25sdk.type5.st25tv;

import com.st.st25sdk.TagHelper;
import com.st.st25sdk.TagHelper.ReadWriteProtection;
import com.st.st25sdk.STException;
import com.st.st25sdk.STException.STExceptionCode;
import com.st.st25sdk.STRegister;
import com.st.st25sdk.command.Iso15693CustomCommand;

import java.util.ArrayList;

import static com.st.st25sdk.TagHelper.ReadWriteProtection.READABLE_AND_WRITABLE;
import static com.st.st25sdk.TagHelper.ReadWriteProtection.READABLE_AND_WRITE_PROTECTED_BY_PWD;
import static com.st.st25sdk.TagHelper.ReadWriteProtection.READ_AND_WRITE_PROTECTED_BY_PWD;
import static com.st.st25sdk.TagHelper.ReadWriteProtection.READ_PROTECTED_BY_PWD_AND_WRITE_IMPOSSIBLE;
import static com.st.st25sdk.STRegister.RegisterAccessRights.REGISTER_READ_WRITE;
import static com.st.st25sdk.STRegister.RegisterDataSize.REGISTER_DATA_ON_8_BITS;



public class ST25TVRegisterArea1SecurityAttribute extends STRegister {


    public static ST25TVRegisterArea1SecurityAttribute newInstance(Iso15693CustomCommand iso15693CustomCommand, byte registerAddress) {


        String registerName = "Area1SecurityAttribute";

        String registerContentDescription =
                "Bits [1:0] : Area 1 access rights\n" +
                        "Bit  [2]   : Memory organization:\n" +
                        "              0: two areas\n" +
                        "              1: single area\n" +
                        "Bits [7:3] : RFU";


        return new ST25TVRegisterArea1SecurityAttribute(
                iso15693CustomCommand,
                registerAddress,
                registerName,
                registerContentDescription,
                REGISTER_READ_WRITE,
                REGISTER_DATA_ON_8_BITS);
    }

    public ST25TVRegisterArea1SecurityAttribute(Iso15693CustomCommand iso15693CustomCommand,
            byte registerAddress,
            String registerName,
            String registerContentDescription,
            RegisterAccessRights registerAccessRights,
            RegisterDataSize registerDataSize) {

        super(iso15693CustomCommand, registerAddress, registerName, registerContentDescription, registerAccessRights, registerDataSize);
        createFieldHash( new ArrayList<STRegisterField>() {
            {
                add(new STRegisterField("RW_PROTECTION_A1","Area 1 access rights\n",0x03));
                add(new STRegisterField("MEM_ORG","0: two areas\n1: single area\n",0x04));
                add(new STRegisterField("RFU","RFU",0xF8));
            }
        });
    }

    /////////// Getters - Setters of the decoded value //////////////
    // Those Getters and Setters are specific to this register

    public ReadWriteProtection getArea1ReadWriteProtection() throws STException {
        TagHelper.ReadWriteProtection readWriteProtection;

        int fieldValue = getRegisterField("RW_PROTECTION_A1").getValue();
        readWriteProtection = getAreaSecurityStatus(fieldValue);

        return readWriteProtection;
    }

    public boolean isMemoryConfiguredInSingleArea() throws STException {
        boolean isMemoryConfiguredInSingleArea;

        int fieldValue = getRegisterField("MEM_ORG").getValue();
        isMemoryConfiguredInSingleArea = (fieldValue == 1);
        return isMemoryConfiguredInSingleArea;
    }

    public void setArea1ReadWriteProtection(TagHelper.ReadWriteProtection area1ReadWriteProtection) throws STException {
        int fieldValue = getRawSecurityStatusValue(area1ReadWriteProtection);
        getRegisterField("RW_PROTECTION_A1").setValue(fieldValue);
    }

    public void setIsMemoryConfiguredInSingleArea(boolean isMemoryConfiguredInSingleArea) throws STException {
        int fieldValue = isMemoryConfiguredInSingleArea ? 1 : 0;
        getRegisterField("MEM_ORG").setValue(fieldValue);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //Private functions allowing the conversion between the "raw value" and the "decoded values"

    /**
     * Function doing the conversion "decoded values" to "raw value".
     *
     * @param readWriteProtection
     * @return
     * @throws STException
     */
    private int getRawSecurityStatusValue(TagHelper.ReadWriteProtection readWriteProtection) throws STException {
        int value;

        switch(readWriteProtection){
            case READABLE_AND_WRITABLE:
                value = 0x00;
                break;
            case READABLE_AND_WRITE_PROTECTED_BY_PWD:
                value = 0x01;
                break;
            case READ_AND_WRITE_PROTECTED_BY_PWD:
                value = 0x02;
                break;
            case READ_PROTECTED_BY_PWD_AND_WRITE_IMPOSSIBLE:
                value = 0x03;
                break;
            default:
                // Value not accepted on ST25TV
                throw new STException(STExceptionCode.BAD_PARAMETER);
        }

        return value;
    }

    /**
     * Private function doing the conversion "raw value" to "decoded value" for the field "ReadWriteProtection"
     * @param value
     * @return
     */
    private TagHelper.ReadWriteProtection getAreaSecurityStatus(int value) {
        TagHelper.ReadWriteProtection readWriteProtection = READABLE_AND_WRITABLE;

        switch(value) {
            case 0x0:
                readWriteProtection = READABLE_AND_WRITABLE;
                break;
            case 0x1:
                readWriteProtection = READABLE_AND_WRITE_PROTECTED_BY_PWD;
                break;
            case 0x2:
                readWriteProtection = READ_AND_WRITE_PROTECTED_BY_PWD;
                break;
            case 0x3:
                readWriteProtection = READ_PROTECTED_BY_PWD_AND_WRITE_IMPOSSIBLE;
                break;
        }

        return readWriteProtection;
    }
}


