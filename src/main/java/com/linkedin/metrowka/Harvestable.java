package com.linkedin.metrowka;

public abstract class Harvestable {

  private final InstrumentType _type;
  private final MeasureUnit _unit;
  private final String _name;

  public Harvestable(InstrumentType type, MeasureUnit unit, String name) {
    super();
    _type = type;
    _name = name;
    _unit = unit;
  }

  abstract public void harvest(Harvester consumer);

  public InstrumentType getType() {
    return _type;
  }

  public String getName() {
    return _name;
  }

  public MeasureUnit getUnit() {
    return _unit;
  }
}
