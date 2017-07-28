package de.metro.robocode;

import java.awt.Color;

import robocode.AdvancedRobot;
import robocode.Condition;
import robocode.CustomEvent;
import robocode.HitByBulletEvent;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class PizzaPower extends AdvancedRobot {
	private static final double WALL_MARGIN = 30;
	
	Enemy enemy = new Enemy();
	static double xForce;
	static double yForce;
	
    @Override
    public void run() {
		setColors(Color.cyan, Color.magenta, Color.cyan);

		addCustomEvent(new Condition("wallApproaching") {
			@Override
			public boolean test() {
				return getX() <= WALL_MARGIN || getX() >= getBattleFieldWidth() - WALL_MARGIN || getY() <= WALL_MARGIN
						|| getY() >= getBattleFieldHeight() - WALL_MARGIN;
			}
		});
		
        while (true) {
        	System.out.println("enemy = " + enemy + " others= " + getOthers());
        	
        	setTurnRadarRight(360);
            
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
    	if (enemy.name == null || enemy.name.equals(e.getName())) {
    		enemy.update(e, getX(), getY(), getHeading());
    	}
    	
    	moveCorners(e);
    	
        fire(1);
    }

    public void moveCorners(ScannedRobotEvent e) {
		double absoluteBearing = e.getBearingRadians() + getHeadingRadians();
		double distance = e.getDistance();

		xForce = xForce * .9 - Math.sin(absoluteBearing) / distance;
		yForce = yForce * .9 - Math.cos(absoluteBearing) / distance;

		setTurnRightRadians(Utils.normalRelativeAngle(Math.atan2(xForce + 1 / getX() - 1 / (getBattleFieldWidth() - getX()),
						yForce + 1 / getY() - 1 / (getBattleFieldHeight() - getY())) - getHeadingRadians()));

		setAhead(Double.POSITIVE_INFINITY);
		setMaxVelocity(420 / getTurnRemaining());
	}
    
    public void onCustomEvent(CustomEvent e) {
		if ("wallApproaching".equals(e.getCondition().getName())) {
			System.out.println("wall " + getX() + " " + getY());
			setMaxVelocity(0);
		}
	}
    
    @Override
    public void onHitWall(HitWallEvent event) {
    	System.out.println("wall hit " + event.getBearing() + " " + event.getTime());
    }
    
    public void onHitByBullet(HitByBulletEvent e) {
        turnLeft(90 - e.getBearing());
    }

	public class Enemy {
		String name;
		double bearing;
		double distance;
		double energy;
		double heading;
		double velocity;
		private double x;
		private double y;

		public void update(ScannedRobotEvent e, double myX, double myY, double myHeading) {
			this.bearing = e.getBearing();
			this.distance = e.getDistance();
			this.energy = e.getEnergy();
			this.heading = e.getHeading();
			this.velocity = e.getVelocity();
			this.name = e.getName();
			

			double absBearingDeg = (myHeading + e.getBearing());
			if (absBearingDeg < 0) {
				absBearingDeg += 360;
			}

			this.x = myX + Math.sin(Math.toRadians(absBearingDeg)) * e.getDistance();
			this.y = myY + Math.cos(Math.toRadians(absBearingDeg)) * e.getDistance();
		}

		@Override
		public String toString() {
			return "Enemy [name=" + name + ", bearing=" + bearing + ", distance=" + distance + ", energy=" + energy
					+ ", heading=" + heading + ", velocity=" + velocity + ", x=" + x + ", y=" + y + "]";
		}
	}
}
