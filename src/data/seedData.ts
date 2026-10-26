import { User, FirstAidGuide, HazardZone, ConnectedDevice, SosAlert, MeshRelayedPacket } from './models/types';

export const SeedUser: User = {
  id: 'local_user',
  name: 'My Device',
  phone: '',
  deviceName: 'ResQLink Device',
  role: 'CITIZEN',
  bloodGroup: null,
  emergencyContact: null,
  medicalNotes: null,
};

export const SeedConnectedDevices: ConnectedDevice[] = [];




export const SeedFirstAidGuides: FirstAidGuide[] = [
  {
    id: 'cpr',
    title: 'CPR (Cardiopulmonary Resuscitation)',
    category: 'Cardiac',
    summary: 'For someone unresponsive and not breathing normally.',
    steps: [
      'Check the scene is safe, then check for a response -- tap shoulders and shout.',
      'Call for emergency help (or send someone) before starting compressions.',
      'Place the heel of one hand on the center of the chest, other hand on top, fingers interlaced.',
      'Push hard and fast: at least 5 cm deep, 100-120 compressions per minute.',
      'Allow full chest recoil between compressions; minimize interruptions.',
      'If trained, give 2 rescue breaths after every 30 compressions.',
      'Continue until emergency responders arrive or the person shows signs of life.',
    ],
    criticalWarning: 'Do not stop compressions for more than 10 seconds at a time.',
    imageUrl: null,
  },
  {
    id: 'severe-bleeding',
    title: 'Severe Bleeding Control',
    category: 'Trauma',
    summary: 'Stopping heavy bleeding from a wound.',
    steps: [
      'Apply firm, direct pressure to the wound with a clean cloth or bandage.',
      'Do not remove the cloth if it soaks through -- add more layers on top.',
      'If on a limb and bleeding does not stop, apply a tourniquet 5-7cm above the wound.',
      'Note the time the tourniquet was applied; do not loosen it once placed.',
      'Keep the person warm and lying down if possible; treat for shock.',
      'Get emergency medical help as soon as possible.',
    ],
    criticalWarning: 'Do not apply a tourniquet over a joint.',
    imageUrl: null,
  },
  {
    id: 'burns',
    title: 'Burns (Thermal)',
    category: 'Trauma',
    summary: 'First response for minor to moderate burns.',
    steps: [
      'Remove the person from the heat source; stop the burning process.',
      'Cool the burn under cool (not ice-cold) running water for 20 minutes.',
      'Remove tight clothing/jewelry near the burn before swelling starts.',
      'Cover loosely with a clean, non-fluffy cloth or cling film.',
      'Do not apply ice, butter, or ointments to the burn.',
      'Seek medical help for burns larger than a palm, on the face/hands/genitals, or if the skin is charred or leathery.',
    ],
    criticalWarning: 'Never use ice directly on a burn -- it can worsen tissue damage.',
    imageUrl: null,
  },
  {
    id: 'choking',
    title: 'Choking (Conscious Adult)',
    category: 'Airway',
    summary: 'When someone cannot cough, speak, or breathe.',
    steps: [
      'Ask "Are you choking?" -- confirm they cannot speak or cough.',
      'Give up to 5 sharp back blows between the shoulder blades with the heel of your hand.',
      'If that does not clear it, give up to 5 abdominal thrusts (Heimlich maneuver).',
      'Alternate 5 back blows and 5 abdominal thrusts until the object is expelled.',
      'If the person becomes unresponsive, lower them to the ground and begin CPR.',
    ],
    criticalWarning: 'Do not perform abdominal thrusts on infants under 1 year -- use back blows and chest thrusts instead.',
    imageUrl: null,
  },
  {
    id: 'fractures',
    title: 'Suspected Fracture',
    category: 'Trauma',
    summary: 'Stabilizing a suspected broken bone before help arrives.',
    steps: [
      'Keep the injured area still -- do not try to realign or straighten it.',
      'Support the area above and below the injury with padding or a splint.',
      'Apply a cold pack wrapped in cloth to reduce swelling, 15-20 minutes at a time.',
      'Check circulation beyond the injury (color, warmth, pulse) periodically.',
      'Do not give food or water in case surgery is needed.',
      'Get medical help, especially if the bone is visibly deformed or has broken skin.',
    ],
    criticalWarning: 'If the bone has pierced the skin, do not push it back in -- cover it loosely and seek immediate help.',
    imageUrl: null,
  },
  {
    id: 'shock',
    title: 'Shock',
    category: 'General',
    summary: 'Recognizing and responding to shock after injury or trauma.',
    steps: [
      'Look for pale, cold, clammy skin, rapid breathing, or confusion.',
      'Lay the person down and raise their legs about 30cm, if no spinal/leg injury is suspected.',
      'Keep them warm with a blanket or coat.',
      'Do not give food or drink.',
      'Loosen tight clothing around the neck, chest, and waist.',
      'Monitor breathing and responsiveness until help arrives.',
    ],
    criticalWarning: null,
    imageUrl: null,
  },
];

const now = Date.now();

export const SeedHazards: HazardZone[] = [];

export const SeedRelayedPackets: MeshRelayedPacket[] = [];

