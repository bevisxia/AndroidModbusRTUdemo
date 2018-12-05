package com.tonyx.androidmodbusrtudemo.modbus;

import java.io.IOException;
import java.util.BitSet;

import android_serialport_api.SerialPort;
import com.tonyx.androidmodbusrtudemo.modbus.exception.ModbusError;
import com.tonyx.androidmodbusrtudemo.modbus.exception.ModbusErrorType;
import com.tonyx.androidmodbusrtudemo.utilities.ByteArrayReader;
import com.tonyx.androidmodbusrtudemo.utilities.ByteArrayWriter;
import com.tonyx.androidmodbusrtudemo.utilities.CRC16;
import com.tonyx.androidmodbusrtudemo.utilities.ThreadUtil;
import com.tonyx.androidmodbusrtudemo.utilities.TimeoutUtil;

/*
    Modbus RTU master
 */
public class ModbusMaster {
    private int timeout = 1000;
    private SerialPort port;

    public ModbusMaster(SerialPort port) {
        this.port = port;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    synchronized public int[] execute(int slave, int function_code, int starting_address, int quantity_of_x, int output_value) throws IOException, ModbusError {
        if (slave < 0 || slave > 255) {
            throw new ModbusError(ModbusErrorType.ModbusInvalidArgumentError, "Invalid slave " + slave);
        }
        if (starting_address < 0 || starting_address > 0xffff) {
            throw new ModbusError(ModbusErrorType.ModbusInvalidArgumentError, "Invalid starting_address " + starting_address);
        }
        if (quantity_of_x < 1 || quantity_of_x > 0xff) {
            throw new ModbusError(ModbusErrorType.ModbusInvalidArgumentError, "Invalid quantity_of_x " + quantity_of_x);
        }

        boolean is_read_function = false;
        int expected_length = 0;
        // 构造request
        ByteArrayWriter request = new ByteArrayWriter();
        request.writeInt8(slave);
        if (function_code == ModbusFunction.READ_COILS || function_code == ModbusFunction.READ_DISCRETE_INPUTS) {
            is_read_function = true;
            request.writeInt8(function_code);
            request.writeInt16(starting_address);
            request.writeInt16(quantity_of_x);

            expected_length = (int) Math.ceil(0.1d * quantity_of_x / 8.0) + 5;
        } else if (function_code == ModbusFunction.READ_INPUT_REGISTERS || function_code == ModbusFunction.READ_HOLDING_REGISTERS) {
            is_read_function = true;
            request.writeInt8(function_code);
            request.writeInt16(starting_address);
            request.writeInt16(quantity_of_x);

            expected_length = 2 * quantity_of_x + 5;
        } else if (function_code == ModbusFunction.WRITE_SINGLE_COIL || function_code == ModbusFunction.WRITE_SINGLE_REGISTER) {
            if (function_code == ModbusFunction.WRITE_SINGLE_COIL)
                if (output_value != 0) output_value = 0xff00;
            request.writeInt8(function_code);
            request.writeInt16(starting_address);
            request.writeInt16(output_value);

            expected_length = 8;
        } else {
            throw new ModbusError(ModbusErrorType.ModbusFunctionNotSupportedError, "Not support function " + function_code);
        }

        byte[] bytes = request.toByteArray();

        int crc = CRC16.compute(bytes);
        request.writeInt16Reversal(crc);
        // 发送到设备
        bytes = request.toByteArray();
        port.getOutputStream().write(bytes);
        // 从设备接收反馈
        byte[] responseBytes;
        try (ByteArrayWriter response = new ByteArrayWriter()) {
            ThreadUtil.sleep(150);
            final int finalExpected_length = expected_length;
            final boolean[] complete = new boolean[1];
            boolean done = TimeoutUtil.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] bytes = new byte[64];
                        while (!complete[0]) {
                            if (port.getInputStream().available() > 0) {
                                int len = port.getInputStream().read(bytes, 0, bytes.length);
                                if (len > 0) {
                                    response.write(bytes, 0, len);
                                    if (response.size() >= finalExpected_length) {
                                        break;
                                    }
                                }
                            }
                            ThreadUtil.sleep(1);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }, timeout);
            complete[0] = true;
            response.flush();

            if (!done) {
                throw new ModbusError(ModbusErrorType.ModbusTimeoutError, String.format("Timeout of %d ms.", timeout));
            }
            responseBytes = response.toByteArray();
        }

        if (responseBytes == null || responseBytes.length != expected_length) {
            throw new ModbusError(ModbusErrorType.ModbusInvalidResponseError, "Response length is invalid " + responseBytes.length);
        }

        ByteArrayReader reader = new ByteArrayReader(responseBytes);
        int responseSlave = reader.readInt8();
        if (responseSlave != slave) {
            throw new ModbusError(ModbusErrorType.ModbusInvalidResponseError,
                    String.format("Response slave %d is different from request slave %d", responseSlave, slave));
        }

        int return_code = reader.readInt8();
        if (return_code > 0x80) {
            int error_code = reader.readInt8();
            throw new ModbusError(error_code);
        }

        int data_length = 0;
        if (is_read_function) {
            // get the values returned by the reading function
            data_length = reader.readInt8();
            int actualLength = responseBytes.length - 5;
            if (data_length != actualLength) {
                throw new ModbusError(ModbusErrorType.ModbusInvalidResponseError,
                        String.format("Byte count is %d while actual number of bytes is %d. ", data_length, actualLength));
            }
        }
        // 读取反馈数据
        int[] result = new int[quantity_of_x];
        if (function_code == ModbusFunction.READ_COILS || function_code == ModbusFunction.READ_DISCRETE_INPUTS) {
            bytes = new byte[data_length];
            for (int i = 0; i < data_length; i++) {
                bytes[i] = (byte) reader.readInt8();
            }
            BitSet bits = BitSet.valueOf(bytes);
            for (int i = 0; i < quantity_of_x; i++) {
                result[i] = bits.get(i) ? 1 : 0;
            }
        } else if (function_code == ModbusFunction.READ_INPUT_REGISTERS || function_code == ModbusFunction.READ_HOLDING_REGISTERS) {
            for (int i = 0; i < quantity_of_x; i++) {
                result[i] = reader.readInt16();
            }
        } else if (function_code == ModbusFunction.WRITE_SINGLE_COIL || function_code == ModbusFunction.WRITE_SINGLE_REGISTER) {
            result[0] = reader.readInt16();
            //result[1] = reader.readInt16();
        }
        return result;
    }

    public int[] readCoils(int slave, int startAddress, int numberOfPoints) throws IOException, ModbusError {
        return execute(slave, ModbusFunction.READ_COILS, startAddress, numberOfPoints, 0);
    }

    public int[] readHoldingRegisters(int slave, int startAddress, int numberOfPoints) throws IOException, ModbusError {
        return execute(slave, ModbusFunction.READ_HOLDING_REGISTERS, startAddress, numberOfPoints, 0);
    }

    public int[] readInputRegisters(int slave, int startAddress, int numberOfPoints) throws IOException, ModbusError {
        return execute(slave, ModbusFunction.READ_INPUT_REGISTERS, startAddress, numberOfPoints, 0);
    }

    public int[] readInputs(int slave, int startAddress, int numberOfPoints) throws IOException, ModbusError {
        return execute(slave, ModbusFunction.READ_DISCRETE_INPUTS, startAddress, numberOfPoints, 0);
    }

    public void writeSingleCoil(int slave, int address, boolean value) throws IOException, ModbusError {
        execute(slave, ModbusFunction.WRITE_SINGLE_COIL, address, 1, value ? 1 : 0);
    }

    public void writeSingleRegister(int slave, int address, int value) throws IOException, ModbusError {
        execute(slave, ModbusFunction.WRITE_SINGLE_REGISTER, address, 1, value);
    }

    public boolean readCoil(int slave, int address) throws IOException, ModbusError {
        int[] values = readCoils(slave, address, 1);
        return values[0] > 0;
    }

    public int readHoldingRegister(int slave, int address) throws IOException, ModbusError {
        int[] values = readHoldingRegisters(slave, address, 1);
        return values[0];
    }

    public int readInputRegister(int slave, int address) throws IOException, ModbusError {
        int[] values = readInputRegisters(slave, address, 1);
        return values[0];
    }

    public boolean readInput(int slave, int address) throws IOException, ModbusError {
        int[] values = readInputs(slave, address, 1);
        return values[0] > 0;
    }
}
