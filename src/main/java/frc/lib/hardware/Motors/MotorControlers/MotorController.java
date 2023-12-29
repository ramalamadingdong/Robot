package frc.lib.hardware.Motors.MotorControlers;

import frc.lib.hardware.HardwareRequirments;

public interface MotorController extends HardwareRequirments {

  void runPercentOut(double num);

  void brakeMode(boolean enable);

  void setInverted(boolean enable);

  int getCanID();

  void disable();
}
