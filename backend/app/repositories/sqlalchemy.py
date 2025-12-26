from __future__ import annotations

from typing import Callable
from uuid import uuid4

from sqlalchemy import Column, ForeignKey, String, create_engine, select
from sqlalchemy.orm import Session, declarative_base, relationship, sessionmaker, selectinload

from ..domain.entities import Area, Building, Location, Zone
from .base import AreaRepository, BuildingRepository, LocationRepository, RepositoryProvider, ZoneRepository

Base = declarative_base()


class LocationModel(Base):
    __tablename__ = "locations"

    id = Column(String, primary_key=True)
    name = Column(String, nullable=False)
    buildings = relationship("BuildingModel", back_populates="location", cascade="all, delete-orphan")


class BuildingModel(Base):
    __tablename__ = "buildings"

    id = Column(String, primary_key=True)
    name = Column(String, nullable=False)
    location_id = Column(String, ForeignKey("locations.id", ondelete="CASCADE"), nullable=False)
    location = relationship("LocationModel", back_populates="buildings")
    zones = relationship("ZoneModel", back_populates="building", cascade="all, delete-orphan")


class ZoneModel(Base):
    __tablename__ = "zones"

    id = Column(String, primary_key=True)
    name = Column(String, nullable=False)
    building_id = Column(String, ForeignKey("buildings.id", ondelete="CASCADE"), nullable=False)
    building = relationship("BuildingModel", back_populates="zones")
    areas = relationship("AreaModel", back_populates="zone", cascade="all, delete-orphan")


class AreaModel(Base):
    __tablename__ = "areas"

    id = Column(String, primary_key=True)
    name = Column(String, nullable=False)
    zone_id = Column(String, ForeignKey("zones.id", ondelete="CASCADE"), nullable=False)
    zone = relationship("ZoneModel", back_populates="areas")


class SQLiteLocationRepository(LocationRepository):
    def __init__(self, session_factory: Callable[[], Session]):
        self._session_factory = session_factory

    def create(self, name: str) -> Location:
        with self._session_factory() as session:
            location = LocationModel(id=str(uuid4()), name=name)
            session.add(location)
            session.commit()
            session.refresh(location)
            return self._to_entity(location)

    def get(self, location_id: str) -> Location | None:
        with self._session_factory() as session:
            stmt = (
                select(LocationModel)
                .options(
                    selectinload(LocationModel.buildings)
                    .selectinload(BuildingModel.zones)
                    .selectinload(ZoneModel.areas)
                )
                .where(LocationModel.id == location_id)
            )
            location = session.execute(stmt).scalars().first()
            return self._to_entity(location) if location else None

    def list(self) -> list[Location]:
        with self._session_factory() as session:
            stmt = select(LocationModel).options(
                selectinload(LocationModel.buildings)
                .selectinload(BuildingModel.zones)
                .selectinload(ZoneModel.areas)
            )
            return [self._to_entity(loc) for loc in session.execute(stmt).scalars().all()]

    def update(self, location: Location) -> Location:
        with self._session_factory() as session:
            model = session.get(LocationModel, location.id)
            if model is None:
                raise KeyError("Location not found")
            model.name = location.name
            session.commit()
            session.refresh(model)
            return self._to_entity(model)

    def delete(self, location_id: str) -> None:
        with self._session_factory() as session:
            location = session.get(LocationModel, location_id)
            if location is None:
                raise KeyError("Location not found")
            session.delete(location)
            session.commit()

    def _to_entity(self, model: LocationModel) -> Location:
        return Location(
            id=model.id,
            name=model.name,
            buildings=[SQLiteBuildingRepository._to_entity_static(b) for b in model.buildings],
        )


class SQLiteBuildingRepository(BuildingRepository):
    def __init__(self, session_factory: Callable[[], Session]):
        self._session_factory = session_factory

    def create(self, name: str, location_id: str) -> Building:
        with self._session_factory() as session:
            if session.get(LocationModel, location_id) is None:
                raise ValueError("Location not found")
            building = BuildingModel(id=str(uuid4()), name=name, location_id=location_id)
            session.add(building)
            session.commit()
            session.refresh(building)
            return self._to_entity(building)

    def get(self, building_id: str) -> Building | None:
        with self._session_factory() as session:
            stmt = select(BuildingModel).options(selectinload(BuildingModel.zones).selectinload(ZoneModel.areas)).where(
                BuildingModel.id == building_id
            )
            building = session.execute(stmt).scalars().first()
            return self._to_entity(building) if building else None

    def list_for_location(self, location_id: str) -> list[Building]:
        with self._session_factory() as session:
            stmt = (
                select(BuildingModel)
                .options(selectinload(BuildingModel.zones).selectinload(ZoneModel.areas))
                .where(BuildingModel.location_id == location_id)
            )
            return [self._to_entity(building) for building in session.execute(stmt).scalars().all()]

    def update(self, building: Building) -> Building:
        with self._session_factory() as session:
            model = session.get(BuildingModel, building.id)
            if model is None:
                raise KeyError("Building not found")
            model.name = building.name
            session.commit()
            session.refresh(model)
            return self._to_entity(model)

    def delete(self, building_id: str) -> None:
        with self._session_factory() as session:
            building = session.get(BuildingModel, building_id)
            if building is None:
                raise KeyError("Building not found")
            session.delete(building)
            session.commit()

    def _to_entity(self, model: BuildingModel) -> Building:
        return self._to_entity_static(model)

    @staticmethod
    def _to_entity_static(model: BuildingModel) -> Building:
        return Building(
            id=model.id,
            name=model.name,
            location_id=model.location_id,
            zones=[SQLiteZoneRepository._to_entity_static(z) for z in model.zones],
        )


class SQLiteZoneRepository(ZoneRepository):
    def __init__(self, session_factory: Callable[[], Session]):
        self._session_factory = session_factory

    def create(self, name: str, building_id: str) -> Zone:
        with self._session_factory() as session:
            if session.get(BuildingModel, building_id) is None:
                raise ValueError("Building not found")
            zone = ZoneModel(id=str(uuid4()), name=name, building_id=building_id)
            session.add(zone)
            session.commit()
            session.refresh(zone)
            return self._to_entity(zone)

    def get(self, zone_id: str) -> Zone | None:
        with self._session_factory() as session:
            stmt = select(ZoneModel).options(selectinload(ZoneModel.areas)).where(ZoneModel.id == zone_id)
            zone = session.execute(stmt).scalars().first()
            return self._to_entity(zone) if zone else None

    def list_for_building(self, building_id: str) -> list[Zone]:
        with self._session_factory() as session:
            stmt = select(ZoneModel).options(selectinload(ZoneModel.areas)).where(ZoneModel.building_id == building_id)
            return [self._to_entity(zone) for zone in session.execute(stmt).scalars().all()]

    def update(self, zone: Zone) -> Zone:
        with self._session_factory() as session:
            model = session.get(ZoneModel, zone.id)
            if model is None:
                raise KeyError("Zone not found")
            model.name = zone.name
            session.commit()
            session.refresh(model)
            return self._to_entity(model)

    def delete(self, zone_id: str) -> None:
        with self._session_factory() as session:
            zone = session.get(ZoneModel, zone_id)
            if zone is None:
                raise KeyError("Zone not found")
            session.delete(zone)
            session.commit()

    def _to_entity(self, model: ZoneModel) -> Zone:
        return self._to_entity_static(model)

    @staticmethod
    def _to_entity_static(model: ZoneModel) -> Zone:
        return Zone(
            id=model.id,
            name=model.name,
            building_id=model.building_id,
            areas=[Area(id=a.id, name=a.name, zone_id=a.zone_id) for a in model.areas],
        )


class SQLiteAreaRepository(AreaRepository):
    def __init__(self, session_factory: Callable[[], Session]):
        self._session_factory = session_factory

    def create(self, name: str, zone_id: str) -> Area:
        with self._session_factory() as session:
            if session.get(ZoneModel, zone_id) is None:
                raise ValueError("Zone not found")
            area = AreaModel(id=str(uuid4()), name=name, zone_id=zone_id)
            session.add(area)
            session.commit()
            session.refresh(area)
            return Area(id=area.id, name=area.name, zone_id=area.zone_id)

    def get(self, area_id: str) -> Area | None:
        with self._session_factory() as session:
            area = session.get(AreaModel, area_id)
            return Area(id=area.id, name=area.name, zone_id=area.zone_id) if area else None

    def list_for_zone(self, zone_id: str) -> list[Area]:
        with self._session_factory() as session:
            stmt = select(AreaModel).where(AreaModel.zone_id == zone_id)
            return [Area(id=a.id, name=a.name, zone_id=a.zone_id) for a in session.execute(stmt).scalars().all()]

    def update(self, area: Area) -> Area:
        with self._session_factory() as session:
            model = session.get(AreaModel, area.id)
            if model is None:
                raise KeyError("Area not found")
            model.name = area.name
            session.commit()
            session.refresh(model)
            return Area(id=model.id, name=model.name, zone_id=model.zone_id)

    def delete(self, area_id: str) -> None:
        with self._session_factory() as session:
            area = session.get(AreaModel, area_id)
            if area is None:
                raise KeyError("Area not found")
            session.delete(area)
            session.commit()


class SQLiteRepositoryProvider:
    def __init__(self, database_url: str):
        self.engine = create_engine(database_url, future=True)
        Base.metadata.create_all(self.engine)
        self._session_factory = sessionmaker(self.engine, expire_on_commit=False)
        self.locations = SQLiteLocationRepository(self._session_factory)
        self.buildings = SQLiteBuildingRepository(self._session_factory)
        self.zones = SQLiteZoneRepository(self._session_factory)
        self.areas = SQLiteAreaRepository(self._session_factory)


def create_sqlite_provider(database_url: str) -> RepositoryProvider:
    return SQLiteRepositoryProvider(database_url)
