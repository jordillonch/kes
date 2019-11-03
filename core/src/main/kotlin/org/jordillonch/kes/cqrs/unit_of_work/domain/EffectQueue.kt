package org.jordillonch.kes.cqrs.unit_of_work.domain

import org.jordillonch.kes.cqrs.Effect
import java.util.Queue

interface EffectQueue : Queue<Effect>
