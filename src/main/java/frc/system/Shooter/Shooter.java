package frc.system.Shooter;

import java.util.function.BooleanSupplier;

import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.VelocityDutyCycle;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;

import edu.wpi.first.networktables.DoubleSubscriber;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.WaitUntilCommand;
import frc.system.MechanismReq;

public class Shooter implements MechanismReq {
    private final String simpleName = this.getClass().getSimpleName();

    // Hardware
    private TalonFX leftMotor;
    private TalonFX rightMotor;

    // Network
    private NetworkTable Table;
    /** Units: RPM */
    private DoubleSubscriber shooterSpeed;
    private DoubleSubscriber shooterSpeedDeadBand;

    // vars
    private BooleanSupplier transitHasNote;
    final VelocityDutyCycle m_request = new VelocityDutyCycle(0);

    public Shooter(NetworkTable networkTable, BooleanSupplier feederNote) {
        this.Table = networkTable.getSubTable(simpleName);

        // Motors
        leftMotor = new TalonFX(14);
        rightMotor = new TalonFX(15);

        // Configs
        TalonFXConfiguration shooterConf = new TalonFXConfiguration();
        shooterConf.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;

        var slot0Configs = new Slot0Configs();
        slot0Configs.kV = 0.18;
        slot0Configs.kA = 0.0;
        slot0Configs.kP = 0.0;
        slot0Configs.kI = 0.0;
        slot0Configs.kD = 0.0;

        leftMotor.getConfigurator().apply(slot0Configs);
        rightMotor.getConfigurator().apply(slot0Configs);

        leftMotor.getConfigurator().apply(shooterConf);
        rightMotor.getConfigurator().apply(shooterConf);

        rightMotor.setControl(new Follower(leftMotor.getDeviceID(), false));

        // TODO: CONFIG and CurrentLimit
        // TODO: make sure that phinox 6 imple is corect

        // Vars
        shooterSpeed = Table.getDoubleTopic("shooterSpeed").subscribe(6000);
        this.Table.getDoubleTopic("shooterSpeed").publish();

        shooterSpeedDeadBand = Table.getDoubleTopic("shooterSpeedDeadBand").subscribe(6000);
        this.Table.getDoubleTopic("shooterSpeedDeadBand").publish();

        this.transitHasNote = feederNote;

        System.out.println("[Init] Creating " + simpleName + " with:");
        System.out.println("\t" + leftMotor.getClass().getSimpleName() + " ID:" + leftMotor.getDeviceID());
        System.out.println("\t" + rightMotor.getClass().getSimpleName() + " ID:" + rightMotor.getDeviceID());

        this.log();
    }

    private void runForward(double speed) {
        leftMotor.setControl(m_request.withVelocity(speed));
    }

    public void disable() {
        leftMotor.disable();
    }

    // Commands
    public Command run() {
        return new Command() {
            public void initialize() {
                runForward(500);
            }

            // public void end(boolean interrupted) {
            // disable();
            // }
        };
    }

    public Command shootSpeaker() {
        return new Command() {
            public void initialize() {
                runForward(shooterSpeed.getAsDouble());
            }

            public boolean isFinished() {
                return !transitHasNote.getAsBoolean();
            }

            // public void end(boolean interrupted) {
            // disable();
            // }
        };
    }

    public Command shootAmp() {
        return new Command() {
            public void initialize() {
                runForward(1000);
            }

            public boolean isFinished() {
                return !transitHasNote.getAsBoolean();
            }

            // public void end(boolean interrupted) {
            // disable();
            // }
        };
    }

    public Command reverse() {
        return new Command() {
            public void initialize() {
                runForward(-100);
            }

            // public void end(boolean interrupted) {
            // disable();
            // }
        };
    }

    public Command waitPrimed() {
        boolean leftMotorAtSpeed = Math
                .abs(leftMotor.getRotorVelocity().getValue() - shooterSpeed.getAsDouble()) <= shooterSpeedDeadBand
                        .getAsDouble();
        boolean rightMotorAtSpeed = Math
                .abs(rightMotor.getRotorVelocity().getValue() - shooterSpeed.getAsDouble()) <= shooterSpeedDeadBand
                        .getAsDouble();
        ;

        return new WaitUntilCommand(() -> {
            return leftMotorAtSpeed && rightMotorAtSpeed;
        });
    }

    // Logging
    public void log() {
        Table.getStringArrayTopic("ControlMode").publish()
                .set(new String[] { leftMotor.getControlMode().toString(), rightMotor.getControlMode().toString() });
        Table.getIntegerArrayTopic("DeviceID").publish()
                .set(new long[] { leftMotor.getDeviceID(), rightMotor.getDeviceID() });

        // Table.getDoubleArrayTopic("Temp").publish()
        // .set(new double[] { leftMotor.getDeviceTemp(), rightMotor.getDeviceTemp() });
        // Table.getDoubleArrayTopic("Supply Current").publish()
        // .set(new double[] { leftMotor.getSupplyCurrent(),
        // rightMotor.getSupplyCurrent() });
        // Table.getDoubleArrayTopic("Stator Current").publish()
        // .set(new double[] { leftMotor.getStatorCurrent(),
        // rightMotor.getStatorCurrent() });
        // Table.getDoubleArrayTopic("Motor Voltage").publish()
        // .set(new double[] { leftMotor.getMotorVoltage(), rightMotor.getMotorVoltage()
        // });
    }

    public void close() throws Exception {
    }
}