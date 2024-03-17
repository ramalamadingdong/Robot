// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.WaitUntilCommand;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.cmd.AimSpeaker;
import frc.config.SwerveConfig;
import frc.system.Swerve;
import frc.system.Intake;
import frc.system.Pivot;
import frc.system.Shooter;
import frc.system.Transit;

public final class Main extends TimedRobot {
	public static void main(String... args) {
		RobotBase.startRobot(Main::new);
	}

	SendableChooser<Command> autonomousChooser = new SendableChooser<>();

	// Controllers
	private final CommandXboxController driver = new CommandXboxController(0);
	private final CommandXboxController operator = new CommandXboxController(1);

	// Subsystems
	private final NetworkTable subsystemsTable = NetworkTableInstance.getDefault().getTable("Subsystems");

	private final Swerve drivetrain = SwerveConfig.swerve;

	private final Transit transit = new Transit(subsystemsTable);
	private final Intake intake = new Intake(subsystemsTable);
	private final Shooter shooter = new Shooter(subsystemsTable);
	private final Pivot pivot = new Pivot(subsystemsTable);

	// Utilities
	static double squareInput(double input) {
		return Math.copySign(input * input, input);
	}

	private Command rumble(RumbleType type, double strength, double duration, CommandXboxController... controllers) {
		Command cmd = new Command() {
			@Override
			public void initialize() {
				for (CommandXboxController controller : controllers)
					controller.getHID().setRumble(type, strength);
			}

			@Override
			public void end(boolean interrupted) {
				for (CommandXboxController controller : controllers)
					controller.getHID().setRumble(type, 0);
			}
		}.withTimeout(duration);

		cmd.addRequirements();
		cmd.setName("Rumble");

		return cmd;
	}

	private void configButtonBindings() {

		// ----------- DEFAULT COMMANDS -----------

		transit.hasNoteTrigger.onTrue(rumble(RumbleType.kBothRumble, 0.5, 0.25));
		transit.hasNoteTrigger.onFalse(rumble(RumbleType.kBothRumble, 0.5, 0.25));

		// Note: it appears that default commands are immediately rescheduled if they
		// finish. Looks like we'll have to implement some special logic to go to the
		// intake by default.
		// pivot.setDefaultCommand(pivot.toIntake());
		pivot.setDefaultCommand(pivot.velocity(operator::getLeftY));

		// ----------- DRIVER CONTROLS -----------

		drivetrain.setDefaultCommand(drivetrain.controllerDrive(
				() -> squareInput(driver.getLeftY()),
				() -> squareInput(driver.getLeftX()),
				// TODO: ask if driver wants turning squared as well
				() -> -driver.getRightX()));

		driver.leftBumper().onTrue(drivetrain.zeroGyro());
		driver.x().whileTrue(drivetrain.brake());
		driver.leftTrigger().onTrue(pivot.intake().andThen(transit.feedIn().deadlineWith(intake.run())));

		// ---------- OPERATOR CONTROLS ----------

		// TODO: Pathfind to the amp using a PathfindToPose command
		operator.leftBumper().whileTrue(pivot.amp().alongWith(shooter.shootAmp()));
		operator.leftTrigger().whileTrue(new AimSpeaker(drivetrain, pivot).alongWith(shooter.shootSpeaker()));
		operator.rightTrigger().whileTrue(transit.runForwards());

		operator.b().whileTrue(transit.runBackward());
		operator.x().whileTrue(intake.reverse());
		operator.y().whileTrue(pivot.subwoofer().alongWith(shooter.shootSpeaker()));
		// Zeroes the pivot, assuming it is at intaking position.
		operator.start().onTrue(new InstantCommand(pivot::zeroToIntake));

		// TODO: Add climb command
	}

	private void configAutos() {
		// ------------------ AUTOS ------------------
		// Adds pathplaner paths. TODO: fix crash here
		// autonomousChooser = drivetrain.getAutoPaths();

		autonomousChooser = new SendableChooser<>();

		// TODO: Ensure a default `null` command is available, and remove this option.
		autonomousChooser.addOption("Nothing", new Command() {
		});

		// TODO: check speed of back-out
		autonomousChooser.addOption("Back-out", drivetrain.controllerDrive(() -> -0.5, () -> 0, () -> 0));

		autonomousChooser.addOption("Shoot",
				new AimSpeaker(drivetrain, pivot).raceWith(
						shooter.shootSpeaker(),
						new WaitUntilCommand(() -> drivetrain.isAimed() && pivot.isAimed())
								.alongWith(shooter.waitPrimed())
								.andThen(transit.feedOut()))
						// TODO: configure the next two as default commands (not working)
						.andThen(shooter.stop().alongWith(pivot.intake())));

		autonomousChooser.addOption("Shoot against subwoofer",
				new WaitCommand(5).andThen(shooter.shootSpeaker().andThen(
						shooter.waitPrimed().andThen(transit.runForwards())).until(() -> !transit.hasNote())));

		SmartDashboard.putData("auto", autonomousChooser);
	}

	private void configNamedCommands() {
		// TODO: re-add these under static factory functions.
		// NamedCommands.registerCommand("Intake", intakeNote);
		// NamedCommands.registerCommand("Shoot", shootNote);
		// NamedCommands.registerCommand("PrimeAmp", primeAmp);
		// NamedCommands.registerCommand("PrimeSpeaker", primeSpeaker);
	}

	// ---------------------------------------------------

	@Override
	public void robotInit() {
		// ------------------- Logging -------------------
		DataLogManager.start();
		DriverStation.startDataLog(DataLogManager.getLog());

		// TODO: re-enable vision once the jitter is solved.
		// new Nt().register(drivetrain);

		configAutos();
		configButtonBindings();
		configNamedCommands();
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

	private Command autonomousCommand;

	@Override
	public void autonomousInit() {
		autonomousCommand = autonomousChooser.getSelected();
		if (autonomousCommand != null)
			autonomousCommand.schedule();
	}

	@Override
	public void autonomousPeriodic() {
	}

	@Override
	public void autonomousExit() {
		if (autonomousCommand != null)
			autonomousCommand.cancel();
	}

	@Override
	public void teleopInit() {
	}

	@Override
	public void teleopPeriodic() {
	}

	@Override
	public void teleopExit() {
	}

	@Override
	public void testInit() {
		// TODO: run full system check so its easy to do pre and post maches
		CommandScheduler.getInstance().cancelAll();
	}

	@Override
	public void testPeriodic() {
	}

	@Override
	public void testExit() {
	}
}
