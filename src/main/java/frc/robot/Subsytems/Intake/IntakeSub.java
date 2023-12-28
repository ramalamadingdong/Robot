package frc.robot.Subsytems.Intake;

import org.littletonrobotics.junction.Logger;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class IntakeSub extends SubsystemBase implements AutoCloseable {
  private final String simpleName = this.getClass().getSimpleName();

  private final IntakeRequirments intakeIO;

  public IntakeSub(IntakeRequirments intakeIO) {
    this.intakeIO = intakeIO;

    System.out.println("[Init] Creating " + simpleName + " with:");
    System.out.println("\t" + intakeIO.getClass().getSimpleName());

    this.intakeIO.setSimpleName(simpleName);

    intakeIO.setBrakeMode(false);
  }

  @Override
  public void periodic() {
    Logger.processInputs(simpleName, intakeIO);

    intakeIO.periodic();
  }

  @Override
  public void simulationPeriodic() {

  }

  // Commands ------------------------------
  public Command intakePice() {
    return run(
        () -> {
          intakeIO.setIntakeSpeed();
        })
        // .onlyIf(intakeIO::isOpen)
        .handleInterrupt(() -> {
          intakeIO.disable();
        });
    // .until(intakeIO::isClosed);
  }

  public Command ejectPice() {
    return run(
        () -> {
          intakeIO.setOutakeSpeed();
        })
        // .onlyIf(intakeIO::isClosed)
        .handleInterrupt(() -> {
          intakeIO.disable();
        });
    // .until(intakeIO::isOpen);
  }

  public Command stopIntake() {
    return run(
        () -> {
          intakeIO.disable();
        });
  }

  // Closing ------------------------------
  @Override
  public void close() throws Exception {
    intakeIO.close();
  }
}
