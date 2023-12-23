package frc.robot;

import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.Subsytems.Intake.*;
import frc.robot.joystics.*;
import org.littletonrobotics.junction.LoggedRobot;
import org.littletonrobotics.junction.Logger;

// https://github.com/Mechanical-Advantage/RobotCode2023/blob/main/src/main/java/org/littletonrobotics/frc2023/subsystems/gripper/GripperIO.java
// https://github.com/Mechanical-Advantage/AdvantageKit/blob/main/docs/INSTALLATION.md
// started to implment advantage kit
public class Robot extends LoggedRobot {
  private Oporator oporator = new SamKeyboard(0);

  private IntakeSub intakeSub;
  private IntakeRequirments IntakeIOHardware;

  @Override
  public void robotInit() {
    // Logger.recordMetadata("ProjectName", "MyProject"); // Set a metadata value
    // // if (isReal()) {
    // // Logger.addDataReceiver(new WPILOGWriter("/U")); // Log to a USB stick
    // // Logger.addDataReceiver(new NT4Publisher()); // Publish data to
    // NetworkTables
    // // powerDistribution = new PowerDistribution(0, ModuleType.kCTRE); // Enables
    // // power
    // // distribution logging
    // // } else {
    // // setUseTiming(false); // Run as fast as possible
    // // String logPath = LogFileUtil.findReplayLog(); // Pull the replay log from
    // // AdvantageScope
    // // (or prompt the user)
    // // Logger.setReplaySource(new WPILOGReader(logPath)); // Read replay log
    // // Logger.addDataReceiver(new WPILOGWriter(LogFileUtil.addPathSuffix(logPath,
    // // "_sim"))); //
    // // Save outputs to a new log
    // // }
    // // Logger.disableDeterministicTimestamps() // See "Deterministic Timestamps"
    // in
    // // the "Understanding Data Flow" page
    // Logger.start(); // Start logging! No more data receivers, replay sources, or
    // metadata values may
    // // be added.
    IntakeIOHardware = new IntakeIOHardware();
    intakeSub = new IntakeSub(IntakeIOHardware);

    configerButtonBindings();
  }

  public void configerButtonBindings() {
    oporator.intakePice().whileTrue(intakeSub.intakePice());
    oporator.intakePice().onFalse(intakeSub.stopIntake());

    oporator.OuttakePice().whileTrue(intakeSub.ejectPice());
    oporator.OuttakePice().onFalse(intakeSub.stopIntake());
  }

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run();
  }

  @Override
  public void disabledInit() {
  }

  @Override
  public void disabledPeriodic() {
  }

  @Override
  public void disabledExit() {
  }

  @Override
  public void autonomousInit() {
  }

  @Override
  public void autonomousPeriodic() {
  }

  @Override
  public void autonomousExit() {
  }

  @Override
  public void teleopInit() {
    IntakeIOHardware.loadPreferences()
  }

  @Override
  public void teleopPeriodic() {
  }

  @Override
  public void teleopExit() {
  }

  @Override
  public void testInit() {
  }

  @Override
  public void testPeriodic() {
  }

  @Override
  public void testExit() {
  }
}
