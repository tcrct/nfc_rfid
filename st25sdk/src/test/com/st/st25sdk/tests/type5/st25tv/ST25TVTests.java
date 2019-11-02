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

package com.st.st25sdk.tests.type5.st25tv;

import static com.st.st25sdk.TagHelper.ReadWriteProtection.READABLE_AND_WRITABLE;
import static com.st.st25sdk.TagHelper.ReadWriteProtection.READ_AND_WRITE_PROTECTED_BY_PWD;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.st.st25sdk.STException;
import com.st.st25sdk.STLog;
import com.st.st25sdk.command.Iso15693CustomKillCommandInterface;
import com.st.st25sdk.tests.generic.NFCTagUtils;
import com.st.st25sdk.tests.type5.Type5TestKill;
import com.st.st25sdk.tests.type5.Type5TestLockSingleBlock;
import com.st.st25sdk.tests.type5.Type5Tests;
import com.st.st25sdk.type5.ST25TVTag;


// This class is for ST25TV specific tests. For Type5 generic tests, use Type5Tests
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ST25TVTests extends Type5Tests {

    private static final String TAG = "ST25TVTests";
    static private ST25TVTag mST25TVTag;


    @BeforeClass
    static public void setUp() {
        // Function called once before all tests
    }

    static public void setTag(ST25TVTag st25TVTag) {
        mST25TVTag = st25TVTag;
        Type5Tests.setTag(st25TVTag);
    }

    /*
     * TEST DESCRIPTION:
     * Test checking the behavior of readRawData() and writeRawData()
     */
    @Test
    @Override
    public void testReadWriteRawData() throws STException {

        NFCTagUtils.printTestName("testReadWriteRawData");

        // Pre-requisite for ST25TV:
        // Configure the ST25TV tag with one single area without protection
        setSingleAreaWithoutProtections();

        // We can now proceed with the test
        super.testReadWriteRawData();
    }

    /*
     * TEST DESCRIPTION:
     * Test checking the behavior of readSingleBlock() and writeSingleBlock()
     */
    @Test
    @Override
    public void testReadWriteSingleBlock() throws STException {

        NFCTagUtils.printTestName("testReadWriteSingleBlock");

        // Pre-requisite for ST25TV:
        // Configure the ST25TV tag with one single area without protection
        setSingleAreaWithoutProtections();

        // We can now proceed with the test
        super.testReadWriteSingleBlock();
    }

    /*
     * TEST DESCRIPTION:
     * Test checking the behavior of readBlocks() and writeBlocks()
     */
    @Test
    @Override
    public void testReadWriteBlocks() throws STException {

        NFCTagUtils.printTestName("testReadWriteBlocks");

        // Pre-requisite for ST25TV:
        // Configure the ST25TV tag with one single area without protection
        setSingleAreaWithoutProtections();

        // We can now proceed with the test
        super.testReadWriteBlocks();
    }

    /*
     * TEST DESCRIPTION:
     * Test reading and writing some NDEF records.
     * It will test that we can:
     * - add a record
     * - update an existing record
     * - and delete a record
     */
    @Test
    @Override
    public void testAddUpdateDeleteNdefRecords() throws STException {

        NFCTagUtils.printTestName("testAddUpdateDeleteNdefRecords");

        // Pre-requisite for ST25TV:
        // Configure the ST25TV tag with one single area without protection
        setSingleAreaWithoutProtections();

        // We can now proceed with the test
        super.testAddUpdateDeleteNdefRecords();
    }

    /*
     * TEST DESCRIPTION:
     * Test reading and writing NDEF record in all the areas of the tags
     */
    @Test
    @Override
    public void testNdefInMultipleAreas() throws STException {

        NFCTagUtils.printTestName("testNdefInMultipleAreas");

        // Pre-requisite for ST25TV:
        // Configure the ST25TV tag with two areas without protection
        setDualAreaWithoutProtections();

        // We can now proceed with the test
        super.testNdefInMultipleAreas();
    }

    /*
     * TEST DESCRIPTION:
     * Test checking the behavior of the tag when writting too much data in an area.
     * It should not corrupt the data in the following area.
     */
    @Override
    @Test
    public void testAreaBoundaries() throws STException {
        NFCTagUtils.printTestName("testMultiAreas");

        // Pre-requisite for ST25TV:
        // Configure the ST25TV tag with two areas without protection
        setDualAreaWithoutProtections();

        // We can now proceed with the test
        super.testAreaBoundaries();
    }

    /*
     * TEST DESCRIPTION:
     * Test checking the behavior of readBytes() and readBlocks() when there are less bytes available
     * than requested. We should return what we have been able to read and not raise an exception.
     *
     * This test checks the behavior at end of memory (when the read asks more bytes than available
     * in memory)
     */
    @Override
    @Test
    public void testIncompleteReadAtEndOfMemory() throws STException {
        NFCTagUtils.printTestName("testIncompleteReadAtEndOfMemory");

        // Pre-requisite for ST25TV:
        // Configure the ST25TV tag with one single area without protection
        setSingleAreaWithoutProtections();

        // We can now proceed with the test
        super.testIncompleteReadAtEndOfMemory();
    }

    /*
     * TEST DESCRIPTION:
     * Test checking the behavior of readBytes() and readBlocks() when there are less bytes available
     * than requested. We should return what we have been able to read and not raise an exception.
     *
     * This test checks the behavior when the following area is not readable. It is executed only for tags
     * supporting MultiAreaInterface.
     */
    @Override
    @Test
    public void testIncompleteReadAtAreaBoundary() throws STException {
        NFCTagUtils.printTestName("testIncompleteReadAtAreaBoundary");

        // Pre-requisite for ST25TV:
        // Configure the ST25TV tag with two areas with AREA1 not protected and AREA2 protected by PWD
        setMultipleAreasWithArea2Protected();

        // We can now proceed with the test
        super.testIncompleteReadAtAreaBoundary();
    }

    /*
     * TEST DESCRIPTION:
     * Test checking that the "kill" command is working
     */
    @Test
    public void testKill() throws STException {
        NFCTagUtils.printTestName("testKill");

        Iso15693CustomKillCommandInterface killInterface = mST25TVTag;
        Type5TestKill.run(mST25TVTag, killInterface);
    }

    /*
     * TEST DESCRIPTION:
     * Test checking that the "Confidential Mode" command is working
     */
    @Test
    public void testConfidentialMode() throws STException {
        NFCTagUtils.printTestName("testConfidentialMode");

        ST25TVTestConfidentialMode.run(mST25TVTag);
    }

    /*
     * TEST DESCRIPTION:
     * Test checking that the "LockSingleBlock" command is working
     */
    @Test
    public void testLockSingleBlock() throws STException {
        NFCTagUtils.printTestName("testLockSingleBlock");

        Type5TestLockSingleBlock.run(mST25TVTag);
    }

    /*
     * TEST DESCRIPTION:
     * Test checking that the Tamper Detect commands are working
     */
    @Test
    public void testTamperDetect() throws STException {
        NFCTagUtils.printTestName("testTamperDetect");

        ST25TVTestTamperDetect.run(mST25TVTag);
    }

    /*
     * TEST DESCRIPTION:
     * Test checking that the EAS commands are working
     */
    @Test
    public void testEas() throws STException {
        NFCTagUtils.printTestName("testEas");

        ST25TVTestEas.run(mST25TVTag);
    }

    /*
     * TEST DESCRIPTION:
     * Test checking that the Counter commands are working
     */
    @Test
    public void testCounter() throws STException {
        NFCTagUtils.printTestName("testCounter");

        ST25TVTestCounter.run(mST25TVTag);
    }

    /***********************************************************************************/

    public void setSingleAreaWithoutProtections() throws STException {
        STLog.i("Configure the ST25TV tag in single area without protections");

        // Write a Config to put the tag in single area, without permissions for Read and Write

        // Config password is needed when we want to change the Config
        byte[] password = new byte[] {0x00, 0x00, 0x00, 0x00};
        ST25TVUtils.presentConfigurationPassword(mST25TVTag, password);

        // We can now set the tag in single area mode
        mST25TVTag.setNumberOfAreas(1);

        // without pwd protection
        mST25TVTag.setReadWriteProtection(1, READABLE_AND_WRITABLE);

    }

    public void setDualAreaWithoutProtections() throws STException {
        STLog.i("Configure the ST25TV tag in dual areas without protections");

        // Write a Config to put the tag in dual area, without permissions for Read and Write

        // Config password is needed when we want to change the Config
        byte[] password = new byte[] {0x00, 0x00, 0x00, 0x00};
        ST25TVUtils.presentConfigurationPassword(mST25TVTag, password);

        // We can now set the tag in dual area mode
        mST25TVTag.setNumberOfAreas(2);

        // without pwd protection for both areas
        mST25TVTag.setReadWriteProtection(1, READABLE_AND_WRITABLE);
        mST25TVTag.setReadWriteProtection(2, READABLE_AND_WRITABLE);

    }

    public void setMultipleAreasWithArea2Protected() throws STException {
        STLog.i("Configure the ST25TV tag in dual areas with AREA1 not protected and AREA2 protected by PWD.");

        // Write a Config to put the tag in dual area, without permissions for Read and Write

        // Config password is needed when we want to change the Config
        byte[] password = new byte[] {0x00, 0x00, 0x00, 0x00};
        ST25TVUtils.presentConfigurationPassword(mST25TVTag, password);

        // We can now set the tag in dual area mode
        mST25TVTag.setNumberOfAreas(2);

        // without pwd protection for both areas
        mST25TVTag.setReadWriteProtection(1, READABLE_AND_WRITABLE);
        mST25TVTag.setReadWriteProtection(2, READ_AND_WRITE_PROTECTED_BY_PWD);

    }

}
