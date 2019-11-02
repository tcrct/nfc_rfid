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

package com.st.st25sdk.command;

import com.st.st25sdk.Helper;
import com.st.st25sdk.RFReaderInterface;
import com.st.st25sdk.STException;
import com.st.st25sdk.STException.STExceptionCode;
import com.st.st25sdk.STLog;
import com.st.st25sdk.type5.ReadBlockResult;
import com.st.st25sdk.type5.Type5Tag;

import java.util.Arrays;

import static com.st.st25sdk.STException.STExceptionCode.BAD_PARAMETER;
import static com.st.st25sdk.STException.STExceptionCode.CMD_FAILED;
import static com.st.st25sdk.STException.STExceptionCode.ISO15693_CMD_NOT_SUPPORTED;
import static com.st.st25sdk.STException.STExceptionCode.TAG_NOT_IN_THE_FIELD;
import static com.st.st25sdk.command.Type5MemoryCommand.CommandSupport.COMMAND_NOT_SUPPORTED;
import static com.st.st25sdk.command.Type5MemoryCommand.CommandSupport.COMMAND_SUPPORTED;
import static com.st.st25sdk.command.Type5MemoryCommand.CommandSupport.INFORMATION_NOT_AVAILABLE_YET;

/**
 * The Type5MemoryCommand class implements the readBlocks/Bytes() and
 * writeBlocks/Bytes() methods of Iso15693Command for Iso15693 tags whatever the tag memory size.
 */
public class Type5MemoryCommand extends Iso15693Protocol implements Type5MemoryCommandInterface {

    protected enum CommandSupport {
        INFORMATION_NOT_AVAILABLE_YET,
        COMMAND_SUPPORTED,
        COMMAND_NOT_SUPPORTED
    }

    //private RFReaderInterface mReaderInterface;
    private Iso15693Command mIso15693Command;


    private CommandSupport isReadMultipleBlockSupported = INFORMATION_NOT_AVAILABLE_YET;
    private CommandSupport isWriteMultipleBlockSupported = INFORMATION_NOT_AVAILABLE_YET;

    public Type5MemoryCommand(RFReaderInterface reader, byte[] uid) {
        this(reader, uid, Iso15693Protocol.DEFAULT_FLAG, Type5Tag.DEFAULT_NBR_OF_BYTES_PER_BLOCK);
    }

    public Type5MemoryCommand(RFReaderInterface reader, byte[] uid, byte flag) {
        this(reader, uid, flag, Type5Tag.DEFAULT_NBR_OF_BYTES_PER_BLOCK);
    }

    public Type5MemoryCommand(RFReaderInterface reader, byte[] uid, int nbrOfBytesPerBlock) {
        this(reader, uid, Iso15693Protocol.DEFAULT_FLAG, nbrOfBytesPerBlock);
    }

    public Type5MemoryCommand(RFReaderInterface reader, byte[] uid, byte flag, int nbrOfBytesPerBlock) {
        super(reader, uid, flag, nbrOfBytesPerBlock);
        mIso15693Command = new Iso15693Command(reader, uid, flag, nbrOfBytesPerBlock);
    }


    /*
     * @param flag
     */
    @Override
    public void setFlag(byte flag) {
        mFlag = flag;
        mIso15693Command.setFlag(flag);
    }

    /*
      Layers on which the readblocks() command is relying:

     |-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
     |                                                                                                                                                                             |
     |                                                                             readblocks()                                                                                    |
     |    This function will try to read blocks through the command readMultipleBlock(). In case of problem, it will then use the readSingleBlocks() command.                      |
     |                                                                                                                                                                             |
     |-----------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
     |                                                                                   |                                                                                         |
     |                           readSingleBlocks()                                      |                                                                                         |
     |  Function reading some blocks by calling single block commands.                   |                                                                                         |
     |                                                                                   |                                                                                         |
     |-----------------------------------------------------------------------------------|                                                                                         |
     |                                                                                   |                                                                                         |
     |                           readSingleBlock()                                       |                               readMultipleBlock()                                       |
     |  Function relying on readSingleBlock() and extendedReadSingleBlock() to read      |  Function relying on readMultipleBlock() and extendedReadMultipleBlock() to read        |
     |  a single block whatever its address (extended or not)                            |  some blocks whatever the address (extended or not)                                     |
     |                                                                                   |                                                                                         |
     |-----------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
     |                                     |                                             |                                        |                                                |
     |  mIso15693Command.readSingleBlock() |  mIso15693Command.extendedReadSingleBlock() |  mIso15693Command.readMultipleBlock()  |  mIso15693Command.extendedReadMultipleBlock()  |
     |                                     |                                             |                                        |                                                |
     -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadBlockResult readBlocks(int firstBlockAddress, int sizeInBlocks) throws STException {
        return readBlocks(firstBlockAddress, sizeInBlocks, mFlag, mUid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadBlockResult readBlocks(int firstBlockAddress, int sizeInBlocks, byte flag, byte[] uid) throws STException {
        int nbrOfBlocksRead = 0;
        int blockSize = mNbrOfBytesPerBlock;
        boolean isReadSuccessful = true;

        // Check flag for Option bit. If set, then the Block
        // Security status byte is sent with each read data.
        if ((flag & OPTION_FLAG) == OPTION_FLAG) {
            blockSize++;
        }

        byte[] buffer = new byte[sizeInBlocks * blockSize];
        Arrays.fill(buffer, (byte) 0xFF);

        // Loop until the number of blocks requested is reached or a read command fails
        while ((nbrOfBlocksRead < sizeInBlocks) && isReadSuccessful) {
            boolean isReadMultipleSuccessful = false;
            int blockAddress = firstBlockAddress + nbrOfBlocksRead;
            int nbrOfRemainingBlocks = sizeInBlocks - nbrOfBlocksRead;
            int nbrOfBlocksToRead = 1;

            if (isReadMultipleBlockSupported() && (nbrOfRemainingBlocks > 1)) {
                /**** Try with readMultipleBlock command ****/
                try {
                    nbrOfBlocksToRead = Math.min(mReaderInterface.getMaxReceiveLengthInBytes() / blockSize, nbrOfRemainingBlocks);

                    byte[] tmpBuf = readMultipleBlock(blockAddress, nbrOfBlocksToRead, flag, uid);
                    if (tmpBuf != null) {
                        // Discard the first Byte containing the command status
                        System.arraycopy(tmpBuf, 1, buffer, nbrOfBlocksRead * blockSize, nbrOfBlocksToRead * blockSize);
                    }

                    nbrOfBlocksRead += nbrOfBlocksToRead;
                    isReadMultipleSuccessful = true;

                    if (isReadMultipleBlockSupported == INFORMATION_NOT_AVAILABLE_YET) {
                        isReadMultipleBlockSupported = COMMAND_SUPPORTED;
                    }

                } catch (STException e) {
                    // readMultipleBlock failed
                    isReadMultipleSuccessful = false;

                    if ((e.getError() == ISO15693_CMD_NOT_SUPPORTED) || (e.getError() == TAG_NOT_IN_THE_FIELD)) {
                        // This tag doesn't support the readMultipleBlock command
                        if (isReadMultipleBlockSupported == INFORMATION_NOT_AVAILABLE_YET) {
                            isReadMultipleBlockSupported = COMMAND_NOT_SUPPORTED;
                        }
                    }
                }
            }

            if (!isReadMultipleSuccessful) {
                /**** Try with readSingleBlock command ****/
                byte[] tmpBuf = readSingleBlocks(blockAddress, nbrOfBlocksToRead, flag, uid);
                // Command successful.
                // WARNING: It may contain less bytes than requested!
                if (tmpBuf != null) {
                    int nbrOfBytesRead = tmpBuf.length;
                    int nbrOfBlocks = nbrOfBytesRead / blockSize;
                    if (nbrOfBlocks != nbrOfBlocksToRead) {
                        // We didn't succeed to read the number of blocks requested
                        isReadSuccessful = false;
                    }
                    System.arraycopy(tmpBuf, 0, buffer, nbrOfBlocksRead * blockSize, nbrOfBytesRead);
                    nbrOfBlocksRead += nbrOfBlocks;

                } else {
                    // No byte was read
                    isReadSuccessful = false;
                }
            }
        }

        if (nbrOfBlocksRead == 0) {
            throw new STException(STExceptionCode.CMD_FAILED);
        }

        // Allocate the object that will contain the result ("data" and potentially "Block Security Status")
        // Warning: We are passing "mNbrOfBytesPerBlock" and not "blockSize" because response.data will not contain the BSS byte
        ReadBlockResult response = new ReadBlockResult(nbrOfBlocksRead, mNbrOfBytesPerBlock);

        // Copy data from buffer to reponse.data and bss to blockSecurityStatus arrays
        if ((flag & OPTION_FLAG) == OPTION_FLAG) {
            for (int blockIndex = 0; blockIndex < nbrOfBlocksRead; blockIndex++) {
                response.blockSecurityStatus[blockIndex] = buffer[blockIndex * blockSize];
                System.arraycopy(buffer, blockIndex * blockSize + 1, response.data, blockIndex * mNbrOfBytesPerBlock, mNbrOfBytesPerBlock);
            }
        } else {
            System.arraycopy(buffer, 0, response.data, 0, response.data.length);
        }

        return response;
    }

    /*
      Layers on which the writeblocks() command is relying:

     |-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
     |                                                                                                                                                                             |
     |                                                                             writeblocks()                                                                                   |
     |    This function will try to write blocks through the command writeMultipleBlock(). In case of problem, it will then use the writeSingleBlocks() command.                   |
     |                                                                                                                                                                             |
     |-----------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
     |                                                                                   |                                                                                         |
     |                           writeSingleBlocks()                                     |                                                                                         |
     |  Function writing some blocks by calling single block commands.                   |                                                                                         |
     |                                                                                   |                                                                                         |
     |-----------------------------------------------------------------------------------|                                                                                         |
     |                                                                                   |                                                                                         |
     |                           writeSingleBlock()                                      |                               writeMultipleBlock()                                      |
     |  Function relying on writeSingleBlock() and extendedWriteSingleBlock() to write   |  Function relying on writeMultipleBlock() and extendedWriteMultipleBlock() to write     |
     |  a single block whatever its address (extended or not)                            |  some blocks whatever the address (extended or not)                                     |
     |                                                                                   |                                                                                         |
     |-----------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
     |                                     |                                             |                                        |                                                |
     |  mIso15693Command.writeSingleBlock()|  mIso15693Command.extendedWriteSingleBlock()|  mIso15693Command.writeMultipleBlock() |  mIso15693Command.extendedWriteMultipleBlock() |
     |                                     |                                             |                                        |                                                |
     -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBlocks(int firstBlockAddress, byte[] data) throws STException {
        writeBlocks(firstBlockAddress, data, mFlag, mUid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBlocks(int firstBlockAddress, byte[] data, byte flag, byte[] uid) throws STException {
        int nbrOfBlocksWritten = 0;
        int nbrOfBlocks = Helper.divisionRoundedUp(data.length, mNbrOfBytesPerBlock);

        // Buffer where will store all the data to write (some 0xFF are added for the case where
        // the data length is not a multiple of block size
        byte[] buffer = new byte[nbrOfBlocks * mNbrOfBytesPerBlock];
        Arrays.fill(buffer, (byte) 0xFF);

        // Copy the data to this buffer
        System.arraycopy(data, 0, buffer, 0, data.length);

        while (nbrOfBlocksWritten < nbrOfBlocks) {
            boolean isWriteMultipleSuccessful = false;
            int blockAddress = firstBlockAddress + nbrOfBlocksWritten;
            int nbrOfRemainingBlocks = nbrOfBlocks - nbrOfBlocksWritten;
            int nbrOfBlocksToWrite = 1;

            if (isWriteMultipleBlockSupported() && (nbrOfRemainingBlocks > 1)) {
                /**** Try with writeMultipleBlock command ****/

                try {
                    // Get max payload size from the reader
                    nbrOfBlocksToWrite = mReaderInterface.getMaxTransmitLengthInBytes() / mNbrOfBytesPerBlock;

                    if ((flag & ADDRESSED_MODE) == ADDRESSED_MODE) {
                        // Remove UID length (8 bytes) from the max transmit size
                        nbrOfBlocksToWrite -= (8 / mNbrOfBytesPerBlock);
                        if (nbrOfBlocksToWrite <= 0) {
                            throw new STException(STExceptionCode.RFREADER_FAILURE);
                        }
                    }

                    // Limit Number of blocks by the max supported by the writeMultipleBlock command
                    nbrOfBlocksToWrite = Math.min(nbrOfBlocksToWrite, nbrOfRemainingBlocks);
                    nbrOfBlocksToWrite = Math.min(nbrOfBlocksToWrite, 4);

                    // The blocks to write are put in a tmp buffer
                    byte[] tmpBuf = new byte[nbrOfBlocksToWrite * mNbrOfBytesPerBlock];
                    System.arraycopy(buffer, nbrOfBlocksWritten * mNbrOfBytesPerBlock, tmpBuf, 0, nbrOfBlocksToWrite * mNbrOfBytesPerBlock);

                    writeMultipleBlock(blockAddress, nbrOfBlocksToWrite, tmpBuf, flag, uid);

                    nbrOfBlocksWritten += nbrOfBlocksToWrite;
                    isWriteMultipleSuccessful = true;

                    if (isWriteMultipleBlockSupported == INFORMATION_NOT_AVAILABLE_YET) {
                        isWriteMultipleBlockSupported = COMMAND_SUPPORTED;
                    }

                } catch (STException e) {
                    // writeMultipleBlock failed
                    isWriteMultipleSuccessful = false;

                    if ((e.getError() == ISO15693_CMD_NOT_SUPPORTED)
                            || (e.getError() == TAG_NOT_IN_THE_FIELD)
                            || (e.getError() == CMD_FAILED)) {
                        // This tag doesn't support the writeMultipleBlock command
                        if (isWriteMultipleBlockSupported == INFORMATION_NOT_AVAILABLE_YET) {
                            isWriteMultipleBlockSupported = COMMAND_NOT_SUPPORTED;
                        }
                    }
                }
            }

            if (!isWriteMultipleSuccessful) {
                /**** Try with writeSingleBlock command ****/

                // The blocks to write are put in a tmp buffer
                byte[] tmpBuf = new byte[nbrOfBlocksToWrite * mNbrOfBytesPerBlock];
                System.arraycopy(buffer, nbrOfBlocksWritten * mNbrOfBytesPerBlock, tmpBuf, 0, nbrOfBlocksToWrite * mNbrOfBytesPerBlock);

                writeSingleBlocks(blockAddress, nbrOfBlocksToWrite, tmpBuf, flag, uid);

                // Write successful
                nbrOfBlocksWritten += nbrOfBlocksToWrite;
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] readBytes(int byteAddress, int sizeInBytes) throws STException{
        return readBytes(byteAddress, sizeInBytes, mFlag, mUid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] readBytes(int byteAddress, int sizeInBytes, byte flag, byte[] uid) throws STException {
        int nbrOfBlocks;
        int blockSize = mNbrOfBytesPerBlock;
        byte[] result;

        if ((byteAddress < 0) || (sizeInBytes <= 0)) {
            throw new STException(BAD_PARAMETER);
        }

        /*
            Memory is organized in blocks of mNbrOfBytesPerBlock Bytes.
            byteAddress and sizeInBytes may not be aligned with block boundaries.
            We will read whole blocks and we will trim the non wanted bytes later.

            |----|----|----|----|----|----|----|----|----|----|----|----|----|
                    |                   |
                    byteAddress         byteAddress+sizeInBytes

                 |firstBlockAddress  |lastBlockAddress
         */

        int firstBlockAddress = byteAddress / mNbrOfBytesPerBlock;
        int nbrOfBytesToSkipInFirstBlock = byteAddress % mNbrOfBytesPerBlock;

        int lastByteAddress = byteAddress + sizeInBytes - 1;
        int lastBlockAddress = lastByteAddress / mNbrOfBytesPerBlock;

        nbrOfBlocks = lastBlockAddress - firstBlockAddress + 1;

        byte[] buffer = new byte[nbrOfBlocks * blockSize];
        Arrays.fill(buffer, (byte) 0xFF);

        ReadBlockResult tmpBuf = readBlocks(firstBlockAddress, nbrOfBlocks, flag, uid);

        // Warning: tmpBuf may contain less bytes than requested

        if (tmpBuf != null && tmpBuf.data != null) {
            if (tmpBuf.data.length == nbrOfBlocks * blockSize) {
                // We have read all the blocks needed

                result = new byte[sizeInBytes];

                // Copy the data and trim the unwanted bytes at the beginning
                System.arraycopy(tmpBuf.data, nbrOfBytesToSkipInFirstBlock, result, 0, result.length);

            } else {
                // We didn't succeed to read all the requested blocks
                // Return what we have been able to read
                int size = tmpBuf.data.length - nbrOfBytesToSkipInFirstBlock;

                result = new byte[size];
                System.arraycopy(tmpBuf.data, nbrOfBytesToSkipInFirstBlock, result, 0, size);
            }
        } else {
            throw new STException(CMD_FAILED);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(int byteAddress, byte[] data) throws STException {
        writeBytes(byteAddress, data, mFlag, mUid);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(int byteAddress, byte[] data, byte flag, byte[] uid) throws STException {
        int nbrOfBlocks;
        int sizeInBytes = data.length;

        if (byteAddress < 0) {
            throw new STException(BAD_PARAMETER);
        }

        /*
            Memory is organized in blocks on mNbrOfBytesPerBlock Bytes.

            byteAddress and data.length may not be aligned with block boundaries so the first and
            last block might be INCOMPLETE. We should read the tag to get the current data in those
            blocks, change some Bytes and write them back in the tag.

            |----|----|----|----|----|----|----|----|----|----|----|----|----|
                    |                   |
                    byteAddress         byteAddress+sizeInBytes

                 |firstBlockAddress  |lastBlockAddress
         */

        int firstBlockAddress = byteAddress / mNbrOfBytesPerBlock;
        int nbrOfBytesToSkipInFirstBlock = byteAddress % mNbrOfBytesPerBlock;

        int lastByteAddress = byteAddress + sizeInBytes - 1;
        int lastBlockAddress = lastByteAddress / mNbrOfBytesPerBlock;

        int nbrOfBytesInLastBlock = 1 + (lastByteAddress % mNbrOfBytesPerBlock);

        nbrOfBlocks = lastBlockAddress - firstBlockAddress + 1;

        // Buffer where will store all the data to write plus a few bytes at the beginning and end
        // in order to be aligned with block boundaries
        byte[] buffer = new byte[nbrOfBlocks * mNbrOfBytesPerBlock];
        Arrays.fill(buffer, (byte) 0xFF);

        if (nbrOfBytesToSkipInFirstBlock != 0) {
            // The first block is going to be partly written so we should read the current data
            ReadBlockResult blockContent = readBlocks(firstBlockAddress, 1, flag, uid);
            byte[] firstBlock = blockContent.data;
            System.arraycopy(firstBlock, 0, buffer, 0, mNbrOfBytesPerBlock);
        }

        if (nbrOfBytesInLastBlock != mNbrOfBytesPerBlock) {
            // The last block is going to be partly written so we should read the current data
            ReadBlockResult blockContent = readBlocks(lastBlockAddress, 1, flag, uid);
            byte[] lastBlock = blockContent.data;
            System.arraycopy(lastBlock, 0, buffer, buffer.length - mNbrOfBytesPerBlock, mNbrOfBytesPerBlock);
        }

        // Copy the data to this buffer
        System.arraycopy(data, 0, buffer, nbrOfBytesToSkipInFirstBlock, data.length);

        // We now have a buffer with a whole number of blocks. Copy them to the tag
        writeBlocks(firstBlockAddress, buffer, flag, uid);
    }

    /**
     * Function reading some blocks by calling single block commands
     * @param blockOffset
     * @param nbrOfBlocks
     * @param flag
     * @param uid
     * @return
     * @throws STException
     */
    private byte[] readSingleBlocks(int blockOffset, int nbrOfBlocks, byte flag, byte[] uid) throws STException {
        int blockSize = mNbrOfBytesPerBlock;

        // Check flag for Option bit. If set, then the Block Security status byte is sent with each read data.
        if ((flag & OPTION_FLAG) == OPTION_FLAG) {
            blockSize++;
        }

        byte[] buffer = new byte[nbrOfBlocks * blockSize];
        Arrays.fill(buffer, (byte) 0xFF);

        for (int block = 0; block < nbrOfBlocks; block++) {
            int blockAddress = blockOffset + block;

            try {
                byte[] tmpBuf = readSingleBlock(blockAddress, flag, uid);
                if (tmpBuf != null) {
                    // Discard the first Byte containing the command status
                    System.arraycopy(tmpBuf, 1, buffer, block * blockSize, blockSize);
                }
            } catch (STException e) {
                // readSingleBlock failed. Return what we have been able to read
                byte[] incompleteData = null;

                if(block > 0) {
                    int nbrOfBytesRead = block * blockSize;

                    // Allocate an array to store what we have been able to read
                    incompleteData = new byte[nbrOfBytesRead];

                    System.arraycopy(buffer, 0, incompleteData, 0, incompleteData.length);
                }

                return incompleteData;
            }
        }

        return buffer;
    }

    /**
     * Function writing some blocks by calling single block commands
     * @param blockOffset
     * @param nbrOfBlocks
     * @param buffer
     * @param flag
     * @param uid
     * @throws STException
     */
    private void writeSingleBlocks(int blockOffset, int nbrOfBlocks, byte[] buffer, byte flag, byte[] uid) throws STException {

        for (int block = 0; block < nbrOfBlocks; block++) {
            int blockAddress = blockOffset + block;

            byte[] tmpBuf = new byte[mNbrOfBytesPerBlock];

            // Copy one block into a tmp buffer
            System.arraycopy(buffer, block * mNbrOfBytesPerBlock, tmpBuf, 0, mNbrOfBytesPerBlock);

            writeSingleBlock(blockAddress, tmpBuf, flag, uid);
        }
    }

    ////////////////// Abstraction of extended or non extended commands ///////////////////////

    /**
     * Local function doing the abstraction between readSingleBlock() and extendedReadSingleBlock()
     * @param blockOffset
     * @param flag
     * @param uid
     * @return
     * @throws STException
     */
    public byte[] readSingleBlock(int blockOffset, byte flag, byte[] uid)  throws STException {
        byte[] result;

        if ((blockOffset < 0) || (blockOffset > 0xFFFF)) {
            throw new STException(BAD_PARAMETER);
        }

        if (blockOffset > 0xFF) {
            result = mIso15693Command.extendedReadSingleBlock(Helper.convertIntTo2BytesHexaFormat(blockOffset), flag, uid);

        } else {
            byte blockAddress;

            blockAddress = (byte) blockOffset;

            result = mIso15693Command.readSingleBlock(blockAddress, flag, uid);
        }

        return result;
    }

    /**
     * Local function doing the abstraction between readMultipleBlock() and extendedReadMultipleBlock()
     * @param blockOffset
     * @param nbrOfBlocks
     * @param flag
     * @param uid
     * @return
     * @throws STException
     */
    private byte[] readMultipleBlock(int blockOffset, int nbrOfBlocks, byte flag, byte[] uid) throws STException {
        byte[] result;
        int expectedResponseLengthInBytes;

        if ((nbrOfBlocks <= 0) || (blockOffset < 0)) {
            throw new STException(BAD_PARAMETER);
        }

        if ((flag & OPTION_FLAG) == OPTION_FLAG) {
            // Each block will contain a block security status on 1 byte so it will be on 'mNbrOfBytesPerBlock + 1' bytes
            expectedResponseLengthInBytes = nbrOfBlocks * (mNbrOfBytesPerBlock + 1) + 1;     // +1 for the status byte of the response
        } else {
            expectedResponseLengthInBytes = nbrOfBlocks * mNbrOfBytesPerBlock + 1;           // +1 for the status byte of the response
        }

        if ((blockOffset + nbrOfBlocks) > 0xFF) {

            // Check that the blocks that we're going to read don't exceed the capacity of 2 Bytes
            if (blockOffset + nbrOfBlocks > 0xFFFF) {
                throw new STException(BAD_PARAMETER);
            }

            result = mIso15693Command.extendedReadMultipleBlock(Helper.convertIntTo2BytesHexaFormat(blockOffset), Helper.convertIntTo2BytesHexaFormat(nbrOfBlocks - 1), flag, uid);

        } else {
            byte blockAddress;
            byte nbrOfBlocksToRead;

            // Check that the last blockAddress doesn't exceed the capacity of a Byte
            if (blockOffset + nbrOfBlocks > 0xFF) {
                throw new STException(BAD_PARAMETER);
            }

            blockAddress = (byte) blockOffset;
            nbrOfBlocksToRead =  (byte) (nbrOfBlocks - 1);

            result = mIso15693Command.readMultipleBlock(blockAddress, nbrOfBlocksToRead, flag, uid);
        }

        if ((result == null) || (result.length != expectedResponseLengthInBytes)) {
            STLog.e("Incorrect response length!");
            throw new STException(CMD_FAILED, result);
        }

        return result;
    }

    /**
     * Local function doing the abstraction between writeSingleBlock() and extendedWriteSingleBlock()
     * @param blockOffset
     * @param buffer
     * @param flag
     * @param uid
     * @return
     * @throws STException
     */
    public byte writeSingleBlock(int blockOffset, byte[] buffer, byte flag, byte[] uid) throws STException {
        byte result;

        if ((blockOffset < 0) || (blockOffset > 0xFFFF)) {
            throw new STException(BAD_PARAMETER);
        }

        if (blockOffset > 0xFF) {
            // Check that the block that we're going to write don't exceed the capacity of 2 Bytes
            if (blockOffset > 0xFFFF) {
                throw new STException(BAD_PARAMETER);
            }

            result = mIso15693Command.extendedWriteSingleBlock(Helper.convertIntTo2BytesHexaFormat(blockOffset), buffer, flag, uid);

        } else {
            byte blockAddress;

            // Check that the block that we're going to write don't exceed the capacity of a Byte
            if (blockOffset > 0xFF) {
                throw new STException(BAD_PARAMETER);
            }

            blockAddress = (byte) blockOffset;

            result = mIso15693Command.writeSingleBlock(blockAddress, buffer, flag, uid);
        }

        return result;
    }

    /**
     * Local function doing the abstraction between writeMultipleBlock() and extendedWriteMultipleBlock()
     * @param blockOffset
     * @param nbrOfBlocks
     * @param flag
     * @param uid
     * @return
     * @throws STException
     */
    private byte writeMultipleBlock(int blockOffset, int nbrOfBlocks, byte[] buffer, byte flag, byte[] uid) throws STException {
        byte result;

        if ((nbrOfBlocks <= 0) || (blockOffset < 0)) {
            throw new STException(BAD_PARAMETER);
        }

        if ((blockOffset + nbrOfBlocks) > 0xFF) {

            // Check that the blocks that we're going to write don't exceed the capacity of 2 Bytes
            if (blockOffset + nbrOfBlocks > 0xFFFF) {
                throw new STException(BAD_PARAMETER);
            }

            result = mIso15693Command.extendedWriteMultipleBlock(Helper.convertIntTo2BytesHexaFormat(blockOffset), Helper.convertIntTo2BytesHexaFormat(nbrOfBlocks - 1), buffer, flag, uid);

        } else {
            byte blockAddress;
            byte nbrOfBlocksToWrite;

            // Check that the last blockAddress doesn't exceed the capacity of a Byte
            if (blockOffset + nbrOfBlocks > 0xFF) {
                throw new STException(BAD_PARAMETER);
            }

            blockAddress = (byte) blockOffset;
            nbrOfBlocksToWrite =  (byte) (nbrOfBlocks - 1);

            result = mIso15693Command.writeMultipleBlock(blockAddress, nbrOfBlocksToWrite, buffer, flag, uid);
        }

        return result;
    }

    private boolean isReadMultipleBlockSupported() {
        return isReadMultipleBlockSupported != COMMAND_NOT_SUPPORTED;
    }

    private boolean isWriteMultipleBlockSupported() {
        return isWriteMultipleBlockSupported != COMMAND_NOT_SUPPORTED;
    }

}
