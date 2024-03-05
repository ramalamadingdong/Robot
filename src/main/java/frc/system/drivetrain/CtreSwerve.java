package frc.system.drivetrain;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import com.ctre.phoenix6.Utils;
import com.ctre.phoenix6.mechanisms.swerve.SwerveDrivetrain;
import com.ctre.phoenix6.mechanisms.swerve.SwerveDrivetrainConstants;
import com.ctre.phoenix6.mechanisms.swerve.SwerveModuleConstants;
import com.ctre.phoenix6.mechanisms.swerve.SwerveRequest;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;
import com.pathplanner.lib.path.PathConstraints;
import com.pathplanner.lib.path.PathPlannerPath;
import com.pathplanner.lib.util.HolonomicPathFollowerConfig;
import com.pathplanner.lib.util.PIDConstants;
import com.pathplanner.lib.util.ReplanningConfig;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Notifier;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj2.command.Command;
import frc.Main;
import frc.config.SwerveConfig;
import frc.system.Drivetrain;
import frc.system.Vision.Measurement;

public class CtreSwerve extends SwerveDrivetrain implements Drivetrain {
    private final PathConstraints constraints;

    private final SwerveRequest.SwerveDriveBrake cachedBrake = new SwerveRequest.SwerveDriveBrake();
    private final SwerveRequest.FieldCentric cachedFieldCentric = new SwerveRequest.FieldCentric();
    private final SwerveRequest.FieldCentricFacingAngle cachedFieldCentricFacing = new SwerveRequest.FieldCentricFacingAngle();
    private final SwerveRequest.ApplyChassisSpeeds AutoRequest = new SwerveRequest.ApplyChassisSpeeds();

    private final AtomicReference<SwerveDriveState> state = new AtomicReference<>();

    private static final double kSimLoopPeriod = 0.005; // 5 ms
    private Notifier m_simNotifier = null;
    private double m_lastSimTime;

    public CtreSwerve(
            PathConstraints constraints,
            double kSpeedAt12VoltsMps,
            SwerveDrivetrainConstants driveTrainConstants,
            SwerveModuleConstants... modules) {
        super(driveTrainConstants, modules);

        registerTelemetry((s) -> state.set(s));

        if (Utils.isSimulation()) {
            startSimThread();
        }

        configurePathPlanner();

        this.constraints = constraints;
    }

    @Override
    public void reset(Pose2d pose) {
        seedFieldRelative(pose);
    }

    @Override
    public void accept(Measurement t) {
        if (t.stdDev() != null) {
            addVisionMeasurement(t.pose().toPose2d(), t.timestamp(), t.stdDev());
        } else {
            addVisionMeasurement(t.pose().toPose2d(), t.timestamp());
        }
    }

    public SendableChooser<Command> getAutoPaths() {
        return AutoBuilder.buildAutoChooser();
    }

    @Override
    public Pose2d pose() {
        return state.get().Pose;
    }

    private ChassisSpeeds getCurrentRobotChassisSpeeds() {
        return m_kinematics.toChassisSpeeds(getState().ModuleStates);
    }

    private void startSimThread() {
        m_lastSimTime = Utils.getCurrentTimeSeconds();

        /* Run simulation at a faster rate so PID gains behave more reasonably */
        m_simNotifier = new Notifier(() -> {
            final double currentTime = Utils.getCurrentTimeSeconds();
            double deltaTime = currentTime - m_lastSimTime;
            m_lastSimTime = currentTime;

            /* use the measured time delta, get battery voltage from WPILib */
            updateSimState(deltaTime, RobotController.getBatteryVoltage());
        });

        m_simNotifier.startPeriodic(kSimLoopPeriod);
    }

    private void configurePathPlanner() {
        double driveBaseRadius = 0;
        for (var moduleLocation : m_moduleLocations) {
            driveBaseRadius = Math.max(driveBaseRadius, moduleLocation.getNorm());
        }

        AutoBuilder.configureHolonomic(
                this::pose, // Supplier of current robot pose
                this::seedFieldRelative, // Consumer for seeding pose against auto
                this::getCurrentRobotChassisSpeeds,
                (speeds) -> this.setControl(AutoRequest.withSpeeds(speeds)), // Consumer of ChassisSpeeds to drive the
                // robot
                new HolonomicPathFollowerConfig(new PIDConstants(10, 0, 0),
                        new PIDConstants(10, 0, 0),
                        SwerveConfig.kSpeedAt12VoltsMps,
                        driveBaseRadius,
                        new ReplanningConfig()),
                () -> {

                    var alliance = DriverStation.getAlliance();
                    if (alliance.isPresent()) {
                        return alliance.get() == DriverStation.Alliance.Red;
                    }
                    return false;
                }, // Change this if the path needs to be flipped on red vs blue
                this); // Subsystem for requirements
        // PPHolonomicDriveController.setRotationTargetOverride(this::getRotationTargetOverride);
    }

    private Optional<Rotation2d> getRotationTargetOverride() {
        // // Some condition that should decide if we want to override rotation
        // if (Limelight.hasGamePieceTarget()) {
        // // Return an optional containing the rotation override (this should be a
        // field
        // // relative rotation)
        // return Optional.of(Limelight.getRobotToGamePieceRotation());
        // } else {
        // // return an empty optional when we don't want to override the path's
        // rotation
        return Optional.empty();
        // }
    }

    // Commands
    private Command applyRequest(Supplier<SwerveRequest> requestSupplier) {
        return run(() -> this.setControl(requestSupplier.get()));
    }

    @Override
    public Command driveTo(Pose2d target, double velocity) {
        return AutoBuilder.pathfindToPose(target, constraints, velocity);
    }

    public Command DriveToThenPath(PathPlannerPath path) {

        return AutoBuilder.pathfindThenFollowPath(path, constraints, 0);
    }

    @Override
    public Command drive(DoubleSupplier xVel, DoubleSupplier yVel, DoubleSupplier rVel) {
        return applyRequest(
                () -> cachedFieldCentric
                        .withVelocityX(xVel.getAsDouble() * SwerveConfig.kSpeedAt12VoltsMps)
                        .withVelocityY(-yVel.getAsDouble() * SwerveConfig.kSpeedAt12VoltsMps)
                        .withRotationalRate(rVel.getAsDouble() * SwerveConfig.kMaxAngularRate));
    }

    @Override
    public Command driveFacingSpeaker(DoubleSupplier xVel, DoubleSupplier yVel) {
        Translation3d vectorToSpeaker = getVectorToSpeaker();
        double yaw = Math.atan2(vectorToSpeaker.getY(), vectorToSpeaker.getX());

        return applyRequest(
                () -> (cachedFieldCentricFacing
                        .withVelocityX(xVel.getAsDouble() * SwerveConfig.kSpeedAt12VoltsMps)
                        .withVelocityY(-yVel.getAsDouble() * SwerveConfig.kSpeedAt12VoltsMps)
                        .withTargetDirection(new Rotation2d(yaw))));
    }

    @Override
    public Command brake() {
        return applyRequest(
                () -> cachedBrake);
    }

    public Translation3d getVectorToSpeaker() {
        Translation3d origin = new Translation3d(pose().getX(), pose().getY(), 0);
        Translation3d mechanism = origin.plus(new Translation3d(1, 1, 1));
        return Main.speakerPosition.minus(mechanism);
    }
}
