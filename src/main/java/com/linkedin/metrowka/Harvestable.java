package com.linkedin.metrowka;

public abstract class Harvestable {

  private final InstrumentType _type;
  private final String _name;

  public Harvestable(InstrumentType type, String name) {
    super();
    _type = type;
    _name = name;
  }

  abstract public void harvest(Harvester consumer);

  public InstrumentType getType() {
    return _type;
  }

  public String getName() {
    return _name;
  }

}
